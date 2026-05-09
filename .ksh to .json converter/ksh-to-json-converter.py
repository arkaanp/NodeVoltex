import json
import sys
import os

def decode_laser_char(c):
    """
    Decodes K-Shoot MANIA base-50 laser characters into a 0.0 - 1.0 float value.
    '0'-'9' -> 0-9
    'A'-'Z' -> 10-35
    'a'-'o' -> 36-50
    """
    if '0' <= c <= '9': return int(c) / 50.0
    if 'A' <= c <= 'Z': return (ord(c) - ord('A') + 10) / 50.0
    if 'a' <= c <= 'o': return (ord(c) - ord('a') + 36) / 50.0
    return None

def convert_ksh_to_json(ksh_path, json_path):
    with open(ksh_path, 'r', encoding='utf-8-sig', errors='replace') as f:
        lines = [line.strip() for line in f.readlines()]

    # General Defaults
    general = {
        "title": "",
        "artist": "",
        "mapper": "",               # from 'effect'
        "illustrator": "",          # Jacket artist
        "difficulty": "light",      # String difficulty name
        "level": 1,                 # Numeric difficulty level
        "baseBpm": "",              # from 't' (Storing as string in case of ranges like "139-150")
        "audioFilename": "audio.mp3", # from 'm'
        "audioVolume": 100,         # from 'mvol'
        "audioOffset": 0,           # from 'o'
        "jacketFilename": "",       # from 'jacket'
        "background": "",           # from 'bg'
        "layer": "",                # from 'layer'
        "previewOffset": 0,         # from 'po'
        "previewLength": 0,         # from 'plength'
        "defaultFilterType": "peak",# from 'filtertype'
        "filterGain": 50,           # from 'pfiltergain'
        "slamAutoVolume": 0,        # from 'chokkakuautovol' (0 or 1)
        "slamVolume": 50            # from 'chokkakuvol'
    }
    
    bpm = 120.0
    beat_num = 4
    beat_den = 4

    hit_objects = []
    lasers = {"left": [], "right": []}

    active_holds = {}
    active_lasers = {"left": None, "right": None}

    # Group lines into header and measure blocks
    blocks = []
    current_block = []
    in_header = True

    for line in lines:
        if line == '--':
            in_header = False
            blocks.append(current_block)
            current_block = []
            continue

        if in_header:
            if '=' in line:
                key, val = line.split('=', 1)
                
                # Metadata & Info
                if key == 'title': general['title'] = val
                elif key == 'artist': general['artist'] = val
                elif key == 'effect': general['mapper'] = val
                elif key == 'illustrator': general['illustrator'] = val
                elif key == 'difficulty': general['difficulty'] = val
                elif key == 'level': general['level'] = int(val) if val.isdigit() else 1
                
                # Audio Settings
                elif key == 't': 
                    general['baseBpm'] = val
                    bpm = float(val.split('-')[0]) # Still grab the initial float for chronological syncing
                elif key == 'm': general['audioFilename'] = val
                elif key == 'mvol': general['audioVolume'] = int(val) if val.isdigit() else 100
                elif key == 'o': general['audioOffset'] = int(val)
                elif key == 'po': general['previewOffset'] = int(val)
                elif key == 'plength': general['previewLength'] = int(val)
                
                # Visuals
                elif key == 'jacket': general['jacketFilename'] = val
                elif key == 'bg': general['background'] = val
                elif key == 'layer': general['layer'] = val
                
                # Effects & Lasers
                elif key == 'filtertype': general['defaultFilterType'] = val
                elif key == 'pfiltergain': general['filterGain'] = int(val) if val.isdigit() else 50
                elif key == 'chokkakuautovol': general['slamAutoVolume'] = int(val) if val.isdigit() else 0
                elif key == 'chokkakuvol': general['slamVolume'] = int(val) if val.isdigit() else 50
                
                # Time Signature
                elif key == 'beat':
                    beat_num, beat_den = map(int, val.split('/'))
        else:
            current_block.append(line)

    if current_block:
        blocks.append(current_block)

    # Audio Sync Initialization
    current_offset = float(general['audioOffset'])
    
    # Trackers for detecting Laser Slams
    absolute_tick = 0
    last_laser_tick = {"left": -2, "right": -2}
    last_laser_offset = {"left": 0.0, "right": 0.0}

    # Process each measure block
    for block in blocks:
        tick_lines_in_block = sum(1 for line in block if '|' in line)
        
        for line in block:
            if line.startswith('t='):
                val = line.split('=')[1]
                bpm = float(val.split('-')[0])
            elif line.startswith('beat='):
                beat_num, beat_den = map(int, line.split('=')[1].split('/'))
            elif '|' in line:
                # Calculate exact duration
                measure_duration = (beat_num * 4.0 / beat_den) * (60000.0 / bpm)
                tick_duration = measure_duration / tick_lines_in_block
                
                offset = current_offset
                parts = line.split('|')
                
                if len(parts) >= 3:
                    bt_str, fx_str, laser_str = parts[0], parts[1], parts[2]

                    # Process BT Notes (Lanes 1-4)
                    for lane_idx in range(4):
                        char = bt_str[lane_idx] if lane_idx < len(bt_str) else '0'
                        lane_num = lane_idx + 1
                        
                        if char == '1': # BT Tap
                            if lane_num in active_holds:
                                hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(offset), "type": "TAP"})
                        elif char == '2': # BT Hold
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(offset), "type": "HOLD", "endTime": int(offset + tick_duration)}
                            else:
                                active_holds[lane_num]["endTime"] = int(offset + tick_duration)
                        else: # '0' or empty
                            if lane_num in active_holds:
                                hit_objects.append(active_holds.pop(lane_num))

                    # Process FX Notes (Lanes 5-6)
                    for lane_idx in range(2):
                        char = fx_str[lane_idx] if lane_idx < len(fx_str) else '0'
                        lane_num = lane_idx + 5
                        
                        if char == '2': # FX Chip (Tap)
                            if lane_num in active_holds:
                                hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(offset), "type": "TAP"})
                        elif char != '0': # FX Hold
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(offset), "type": "HOLD", "endTime": int(offset + tick_duration)}
                            else:
                                active_holds[lane_num]["endTime"] = int(offset + tick_duration)
                        else: # '0'
                            if lane_num in active_holds:
                                hit_objects.append(active_holds.pop(lane_num))

                    # Process Lasers (with Slam Detection)
                    for l_idx, l_key in enumerate(["left", "right"]):
                        if l_idx < len(laser_str):
                            char = laser_str[l_idx]
                            
                            if char == '-': # Laser Ends
                                if active_lasers[l_key] is not None:
                                    lasers[l_key].append(active_lasers[l_key])
                                    active_lasers[l_key] = None
                            
                            elif char != ':': # Laser Point Detected
                                val = decode_laser_char(char)
                                if val is not None:
                                    if active_lasers[l_key] is None:
                                        # Store the BPM so we can calculate the 1/4th beat later if it's a slam
                                        active_lasers[l_key] = {"nodes": [], "bpm": bpm}
                                    
                                    node_offset = offset
                                    
                                    # SLAM LOGIC: If the very last tick also had a laser node, snap timing
                                    if absolute_tick == last_laser_tick[l_key] + 1:
                                        node_offset = last_laser_offset[l_key]
                                    
                                    # Append the node
                                    active_lasers[l_key]["nodes"].append({
                                        "offset": float(node_offset),
                                        "x": val
                                    })
                                    
                                    # Track states for next tick
                                    last_laser_tick[l_key] = absolute_tick
                                    last_laser_offset[l_key] = node_offset

                current_offset += tick_duration
                absolute_tick += 1 # Progress master tick counter

    # Cap trailing objects that didn't close properly at EOF
    for l_key in ["left", "right"]:
        if active_lasers[l_key] is not None:
            lasers[l_key].append(active_lasers[l_key])

    for h in active_holds.values():
        hit_objects.append(h)

    # --- NEW: POST-PROCESS 2-POINT SLAMS ---
    for l_key in ["left", "right"]:
        for segment in lasers[l_key]:
            nodes = segment["nodes"]
            # If the segment has exactly 2 points, and both occur at the exact same offset (a slam)
            if len(nodes) == 2 and nodes[0]["offset"] == nodes[1]["offset"]:
                seg_bpm = segment.get("bpm", 120.0)
                
                # A full beat in ms is (60000 / BPM). A 1/4th beat (16th note) is that divided by 4.
                # *Note: If you meant a 1/4 Note (which is 1 full beat), remove the "/ 4.0"*
                quarter_beat_ms = (60000.0 / seg_bpm) / 4.0
                
                slam_offset = nodes[0]["offset"]
                start_x = nodes[0]["x"]
                end_x = nodes[1]["x"]
                
                # Expand the 2 points into 4 points with vertical bounds
                segment["nodes"] = [
                    {"offset": max(0.0, slam_offset - quarter_beat_ms), "x": start_x},
                    {"offset": slam_offset, "x": start_x},
                    {"offset": slam_offset, "x": end_x},
                    {"offset": slam_offset + quarter_beat_ms, "x": end_x}
                ]
            
            # Clean up the temporary BPM marker
            if "bpm" in segment:
                del segment["bpm"]


    # Sort chronologically
    hit_objects.sort(key=lambda x: x["startTime"])

    output_data = {
        "general": general,
        "hitObjects": hit_objects,
        "lasers": lasers
    }

    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2)

    print(f"Successfully converted '{ksh_path}' to '{json_path}'.")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python ksh_converter.py <input.ksh> <output.json>")
    else:
        input_file = sys.argv[1]
        output_file = sys.argv[2]
        
        if os.path.exists(input_file):
            convert_ksh_to_json(input_file, output_file)
        else:
            print(f"Error: {input_file} does not exist.")