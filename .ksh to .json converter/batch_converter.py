import json
import sys
import os
import shutil

def decode_laser_char(c):
    """
    Decodes K-Shoot MANIA base-50 laser characters into a 0.0 - 1.0 float value.
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
                if key == 'title': general['title'] = val
                elif key == 'artist': general['artist'] = val
                elif key == 'effect': general['mapper'] = val
                elif key == 'level': general['level'] = int(val) if val.isdigit() else 1
                elif key == 'm': general['audioFilename'] = val
                elif key == 'o': general['audioOffset'] = int(val)
                elif key == 't': 
                    bpm = float(val.split('-')[0])
                elif key == 'beat':
                    beat_num, beat_den = map(int, val.split('/'))
        else:
            current_block.append(line)

    if current_block:
        blocks.append(current_block)

    current_offset = float(general['audioOffset'])
    
    absolute_tick = 0
    last_laser_tick = {"left": -2, "right": -2}
    last_laser_offset = {"left": 0.0, "right": 0.0}

    for block in blocks:
        tick_lines_in_block = sum(1 for line in block if '|' in line)
        
        for line in block:
            if line.startswith('t='):
                val = line.split('=')[1]
                bpm = float(val.split('-')[0])
            elif line.startswith('beat='):
                beat_num, beat_den = map(int, line.split('=')[1].split('/'))
            elif '|' in line:
                measure_duration = (beat_num * 4.0 / beat_den) * (60000.0 / bpm)
                tick_duration = measure_duration / tick_lines_in_block
                
                offset = current_offset
                parts = line.split('|')
                
                if len(parts) >= 3:
                    bt_str, fx_str, laser_str = parts[0], parts[1], parts[2]

                    for lane_idx in range(4):
                        char = bt_str[lane_idx] if lane_idx < len(bt_str) else '0'
                        lane_num = lane_idx + 1
                        
                        if char == '1':
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(offset), "type": "TAP"})
                        elif char == '2':
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(offset), "type": "HOLD", "endTime": int(offset + tick_duration)}
                            else:
                                active_holds[lane_num]["endTime"] = int(offset + tick_duration)
                        else:
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))

                    for lane_idx in range(2):
                        char = fx_str[lane_idx] if lane_idx < len(fx_str) else '0'
                        lane_num = lane_idx + 5
                        
                        if char == '2':
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(offset), "type": "TAP"})
                        elif char != '0':
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(offset), "type": "HOLD", "endTime": int(offset + tick_duration)}
                            else:
                                active_holds[lane_num]["endTime"] = int(offset + tick_duration)
                        else:
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))

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
                                        active_lasers[l_key] = {"nodes": [], "bpm": bpm}
                                    
                                    node_offset = offset
                                    
                                    if absolute_tick == last_laser_tick[l_key] + 1:
                                        node_offset = last_laser_offset[l_key]
                                    
                                    active_lasers[l_key]["nodes"].append({
                                        "offset": float(node_offset),
                                        "x": val
                                    })
                                    
                                    last_laser_tick[l_key] = absolute_tick
                                    last_laser_offset[l_key] = node_offset

                current_offset += tick_duration
                absolute_tick += 1

    for l_key in ["left", "right"]:
        if active_lasers[l_key] is not None:
            lasers[l_key].append(active_lasers[l_key])

    for h in active_holds.values():
        hit_objects.append(h)

    # Post-process 2-point slams
    for l_key in ["left", "right"]:
        for segment in lasers[l_key]:
            nodes = segment["nodes"]
            if len(nodes) == 2 and nodes[0]["offset"] == nodes[1]["offset"]:
                seg_bpm = segment.get("bpm", 120.0)
                quarter_beat_ms = (60000.0 / seg_bpm) / 4.0
                
                slam_offset = nodes[0]["offset"]
                start_x = nodes[0]["x"]
                end_x = nodes[1]["x"]
                
                segment["nodes"] = [
                    {"offset": max(0.0, slam_offset - quarter_beat_ms), "x": start_x},
                    {"offset": slam_offset, "x": start_x},
                    {"offset": slam_offset, "x": end_x},
                    {"offset": slam_offset + quarter_beat_ms, "x": end_x}
                ]
            if "bpm" in segment:
                del segment["bpm"]

    hit_objects.sort(key=lambda x: x["startTime"])

    output_data = {
        "general": general,
        "hitObjects": hit_objects,
        "lasers": lasers
    }

    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2)

def run_batch_conversion():
    base_dir = os.getcwd()
    export_dir = os.path.join(base_dir, "Export")
    
    print(f"Starting batch conversion in: {base_dir}")
    os.makedirs(export_dir, exist_ok=True)

    folders_processed = 0

    # Iterate through all items in the current directory
    for item in os.listdir(base_dir):
        source_folder = os.path.join(base_dir, item)

        # Skip the Export folder itself and any files (we only want directories)
        if not os.path.isdir(source_folder) or item == "Export":
            continue

        # Find .ksh files in this directory
        ksh_files = [f for f in os.listdir(source_folder) if f.lower().endswith('.ksh')]
        
        if not ksh_files:
            continue # Skip folders that don't have .ksh files

        folders_processed += 1
        print(f"\nProcessing folder: '{item}'")
        
        # Create corresponding destination folder inside /Export
        dest_folder = os.path.join(export_dir, item)
        os.makedirs(dest_folder, exist_ok=True)

        # 1. Convert all .ksh files
        for ksh_file in ksh_files:
            ksh_path = os.path.join(source_folder, ksh_file)
            json_filename = os.path.splitext(ksh_file)[0] + '.json'
            json_path = os.path.join(dest_folder, json_filename)
            
            try:
                convert_ksh_to_json(ksh_path, json_path)
                print(f"  [+] Converted: {ksh_file} -> {json_filename}")
            except Exception as e:
                print(f"  [!] Error converting {ksh_file}: {e}")

        # 2. Copy audio files (.mp3 and .ogg)
        audio_files = [f for f in os.listdir(source_folder) if f.lower().endswith(('.mp3', '.ogg'))]
        for audio_file in audio_files:
            src_audio = os.path.join(source_folder, audio_file)
            dst_audio = os.path.join(dest_folder, audio_file)
            
            # Copy only if it doesn't already exist to save time on reruns
            if not os.path.exists(dst_audio):
                shutil.copy2(src_audio, dst_audio)
                print(f"  [>] Copied audio: {audio_file}")
            else:
                print(f"  [=] Audio already exists: {audio_file}")

    print(f"\nBatch processing complete! Processed {folders_processed} folder(s).")
    print(f"Check the '{export_dir}' folder for your files.")

if __name__ == "__main__":
    run_batch_conversion()