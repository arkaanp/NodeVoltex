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

    # Updated Initialization / Defaults based on the new general format
    general = {
        "title": "",
        "artist": "",
        "mapper": "",
        "level": 1,
        "audioFilename": "audio.mp3",
        "audioOffset": 0
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
                # Map standard .ksh header fields to the requested JSON keys
                if key == 'title': general['title'] = val
                elif key == 'artist': general['artist'] = val
                elif key == 'effect': general['mapper'] = val
                elif key == 'level': general['level'] = int(val) if val.isdigit() else 1
                elif key == 'm': general['audioFilename'] = val
                elif key == 'o': general['audioOffset'] = int(val)
                elif key == 't': 
                    bpm = float(val.split('-')[0]) # Use initial if a range is present
                elif key == 'beat':
                    beat_num, beat_den = map(int, val.split('/'))
        else:
            current_block.append(line)

    if current_block:
        blocks.append(current_block)

    # Syncing: Initialize current_offset with the audioOffset from the header
    current_offset = float(general['audioOffset'])

    # Process each measure block
    for block in blocks:
        # Count only the tick rows for proper subdivision duration
        tick_lines_in_block = sum(1 for line in block if '|' in line)
        
        for line in block:
            if line.startswith('t='):
                val = line.split('=')[1]
                bpm = float(val.split('-')[0])
            elif line.startswith('beat='):
                beat_num, beat_den = map(int, line.split('=')[1].split('/'))
            elif '|' in line:
                # Calculate the exact duration of this tick based on current BPM
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
                        elif char != '0': # FX Hold (1, 3, A, B, etc.)
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(offset), "type": "HOLD", "endTime": int(offset + tick_duration)}
                            else:
                                active_holds[lane_num]["endTime"] = int(offset + tick_duration)
                        else: # '0'
                            if lane_num in active_holds:
                                hit_objects.append(active_holds.pop(lane_num))

                    # Process Lasers
                    for l_idx, l_key in enumerate(["left", "right"]):
                        if l_idx < len(laser_str):
                            char = laser_str[l_idx]
                            if char == '-':
                                if active_lasers[l_key] is not None:
                                    lasers[l_key].append(active_lasers[l_key])
                                    active_lasers[l_key] = None
                            elif char != ':':
                                val = decode_laser_char(char)
                                if val is not None:
                                    if active_lasers[l_key] is None:
                                        active_lasers[l_key] = {"nodes": []}
                                    active_lasers[l_key]["nodes"].append({"offset": int(offset), "x": val})

                # Increment precisely by one tick chronologically
                current_offset += tick_duration

    # Cap trailing objects that never closed properly at the end of the file
    for l_key in ["left", "right"]:
        if active_lasers[l_key] is not None:
            lasers[l_key].append(active_lasers[l_key])

    for h in active_holds.values():
        hit_objects.append(h)

    # Sort chronologically to keep the JSON organized cleanly
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