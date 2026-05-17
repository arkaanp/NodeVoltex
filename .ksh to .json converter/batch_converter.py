import json
import sys
import os
import shutil

def decode_laser_char(c):
    """
    Decodes K-Shoot MANIA base-62 laser characters into a base 0.0 - 1.0 float value.
    """
    val = None
    if '0' <= c <= '9': val = ord(c) - ord('0')
    elif 'A' <= c <= 'Z': val = ord(c) - ord('A') + 10
    elif 'a' <= c <= 'z': val = ord(c) - ord('a') + 36
    
    if val is not None:
        return val / 50.0
    return None

def map_difficulty_to_filename(diff_str):
    diff_str = diff_str.lower().strip()
    mapping = {
        'light': 'nov', 'challenge': 'adv', 'extended': 'exh', 'infinite': 'mxm',
        'novice': 'nov', 'advanced': 'adv', 'exhaust': 'exh', 'maximum': 'mxm',
        'gravity': 'mxm', 'heavenly': 'mxm', 'vivid': 'mxm', 'exceed': 'mxm'
    }
    return mapping.get(diff_str, diff_str) + '.json'

def convert_ksh_to_json(ksh_path, dest_folder):
    with open(ksh_path, 'r', encoding='utf-8-sig', errors='replace') as f:
        lines = [line.strip() for line in f.readlines()]

    general = {
        "title": "", "artist": "", "mapper": "", "illustrator": "",
        "difficulty": "light", "level": 1, "baseBpm": "",
        "audioFilename": "audio.mp3", "audioVolume": 100, "audioOffset": 0,
        "jacketFilename": "", "background": "", "layer": "",
        "previewOffset": 0, "previewLength": 0, "defaultFilterType": "peak",
        "filterGain": 50, "slamAutoVolume": 0, "slamVolume": 50
    }
    
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
                elif key == 'illustrator': general['illustrator'] = val
                elif key == 'difficulty': general['difficulty'] = val
                elif key == 'level': general['level'] = int(val) if val.isdigit() else 1
                elif key == 't': general['baseBpm'] = val
                elif key == 'm': general['audioFilename'] = val
                elif key == 'mvol': general['audioVolume'] = int(val) if val.isdigit() else 100
                elif key == 'o': general['audioOffset'] = int(val)
                elif key == 'po': general['previewOffset'] = int(val)
                elif key == 'plength': general['previewLength'] = int(val)
                elif key == 'jacket': general['jacketFilename'] = val
                elif key == 'bg': general['background'] = val
                elif key == 'layer': general['layer'] = val
                elif key == 'filtertype': general['defaultFilterType'] = val
                elif key == 'pfiltergain': general['filterGain'] = int(val) if val.isdigit() else 50
                elif key == 'chokkakuautovol': general['slamAutoVolume'] = int(val) if val.isdigit() else 0
                elif key == 'chokkakuvol': general['slamVolume'] = int(val) if val.isdigit() else 50
        else:
            current_block.append(line)

    if current_block:
        blocks.append(current_block)

    bpm = float(general['baseBpm'].split('-')[0]) if general['baseBpm'] else 120.0
    current_offset = float(general['audioOffset'])
    
    active_holds = {}
    hit_objects = []
    flat_ticks = []

    # Laser State Trackers
    laser_active_l = False
    laser_active_r = False
    laser_scale_l = 1.0
    laser_scale_r = 1.0

    for block in blocks:
        tick_lines_in_block = sum(1 for line in block if '|' in line)
        for line in block:
            if line.startswith('t='):
                bpm = float(line.split('=')[1].split('-')[0])
            elif line.startswith('beat='):
                beat_num, beat_den = map(int, line.split('=')[1].split('/'))
            
            # Explicit Laser Range Multipliers
            elif line.startswith('laserrange_l='):
                raw_scale = float(line.split('=')[1].replace('x', ''))
                laser_scale_l = 1.0 + (raw_scale - 1.0) * 0.5  # Squishes 2x down to 1.5x
                
            elif line.startswith('laserrange_r='):
                raw_scale = float(line.split('=')[1].replace('x', ''))
                laser_scale_r = 1.0 + (raw_scale - 1.0) * 0.5  # Squishes 2x down to 1.5x
                
            elif '|' in line:
                measure_duration = (beat_num * 4.0 / beat_den) * (60000.0 / bpm)
                tick_duration = measure_duration / tick_lines_in_block
                
                parts = line.split('|')
                if len(parts) >= 3:
                    bt_str, fx_str, laser_str = parts[0], parts[1], parts[2]

                    for lane_idx in range(4):
                        char = bt_str[lane_idx] if lane_idx < len(bt_str) else '0'
                        lane_num = lane_idx + 1
                        if char == '1':
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(round(current_offset)), "type": "TAP"})
                        elif char == '2':
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(round(current_offset)), "type": "HOLD", "endTime": int(round(current_offset + tick_duration))}
                            else:
                                active_holds[lane_num]["endTime"] = int(round(current_offset + tick_duration))
                        else:
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))

                    for lane_idx in range(2):
                        char = fx_str[lane_idx] if lane_idx < len(fx_str) else '0'
                        lane_num = lane_idx + 5
                        if char == '2':
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))
                            hit_objects.append({"lane": lane_num, "startTime": int(round(current_offset)), "type": "TAP"})
                        elif char != '0':
                            if lane_num not in active_holds:
                                active_holds[lane_num] = {"lane": lane_num, "startTime": int(round(current_offset)), "type": "HOLD", "endTime": int(round(current_offset + tick_duration))}
                            else:
                                active_holds[lane_num]["endTime"] = int(round(current_offset + tick_duration))
                        else:
                            if lane_num in active_holds: hit_objects.append(active_holds.pop(lane_num))

                    char_l = laser_str[0] if len(laser_str) > 0 else '0'
                    char_r = laser_str[1] if len(laser_str) > 1 else '0'
                    
                    # NEW: Auto-Reset Laser Scale to 1x when the laser segment ends ('-')
                    if char_l == '-':
                        if laser_active_l:
                            laser_scale_l = 1.0
                            laser_active_l = False
                    else:
                        laser_active_l = True

                    if char_r == '-':
                        if laser_active_r:
                            laser_scale_r = 1.0
                            laser_active_r = False
                    else:
                        laser_active_r = True
                    
                    flat_ticks.append({
                        "offset": current_offset,
                        "duration": tick_duration,
                        "bpm": bpm,
                        "l": char_l,
                        "r": char_r,
                        "scale_l": laser_scale_l, 
                        "scale_r": laser_scale_r
                    })
                current_offset += tick_duration

    for h in active_holds.values(): hit_objects.append(h)
    hit_objects.sort(key=lambda x: x["startTime"])

    lasers = {"left": [], "right": []}
    
    for lane_key, char_key, scale_key in [("left", "l", "scale_l"), ("right", "r", "scale_r")]:
        current_segment = None
        last_val = None
        last_tick_offset = None
        
        for tick in flat_ticks:
            char = tick[char_key]
            offset = tick["offset"]
            bpm = tick["bpm"]
            scale = tick[scale_key]
            
            if char == '-':
                if current_segment is not None:
                    if last_tick_offset is not None and last_tick_offset > current_segment["nodes"][-1]["offset"]:
                        current_segment["nodes"].append({"offset": round(last_tick_offset, 6), "x": last_val})
                    lasers[lane_key].append(current_segment)
                    current_segment = None
            elif char == ':':
                if current_segment is not None:
                    last_tick_offset = offset
            elif char != '0' and not char.isalnum():
                pass 
            else:
                val = decode_laser_char(char)
                if val is not None:
                    # Mathematical expansion based on current laser range state
                    val = 0.5 + (val - 0.5) * scale
                    val = round(val, 6) 
                    
                    if current_segment is None:
                        current_segment = {"nodes": [], "bpm": bpm}
                    
                    current_segment["nodes"].append({"offset": round(offset, 6), "x": val})
                    last_val = val
                    last_tick_offset = offset

        if current_segment is not None:
            if last_tick_offset is not None and last_tick_offset > current_segment["nodes"][-1]["offset"]:
                current_segment["nodes"].append({"offset": round(last_tick_offset, 6), "x": last_val})
            lasers[lane_key].append(current_segment)

    for l_key in ["left", "right"]:
        for segment in lasers[l_key]:
            nodes = segment["nodes"]
            if not nodes: continue
                
            seg_bpm = segment.get("bpm", 120.0)
            quarter_beat_ms = (60000.0 / seg_bpm) / 4.0
            
            # Pure Isolated 2-Point Slams (35ms extensions applied)
            if len(nodes) == 2:
                dt = nodes[1]["offset"] - nodes[0]["offset"]
                dx = nodes[1]["x"] - nodes[0]["x"]
                if 0 < dt <= 150 and abs(dx) > 0:
                    slam_dur = min(dt, 35.0) 
                    extension_ms = 35.0  # <--- SET YOUR CUSTOM LENGTH HERE
                    
                    t1 = nodes[0]["offset"]
                    x1 = nodes[0]["x"]
                    x2 = nodes[1]["x"]
                    segment["nodes"] = [
                        {"offset": round(max(0.0, t1 - extension_ms), 6), "x": x1},
                        {"offset": round(t1, 6), "x": x1},
                        {"offset": round(t1 + slam_dur, 6), "x": x2},
                        {"offset": round(t1 + slam_dur + extension_ms, 6), "x": x2}
                    ]
                continue
            
            new_nodes = [nodes[0]]
            for i in range(1, len(nodes)):
                prev = new_nodes[-1] 
                curr = nodes[i]
                
                dt = curr["offset"] - prev["offset"]
                dx = curr["x"] - prev["x"]
                
                is_slam = False
                if 0 < dt <= 150 and abs(dx) >= 0.15:
                    prev_flat = (i > 1) and (abs(nodes[i-1]["x"] - nodes[i-2]["x"]) < 0.01)
                    next_flat = (i < len(nodes) - 1) and (abs(nodes[i+1]["x"] - curr["x"]) < 0.01)
                    
                    prev_dir_change = (i > 1) and ( (nodes[i-1]["x"] - nodes[i-2]["x"]) * dx < 0 )
                    next_dir_change = (i < len(nodes) - 1) and ( (nodes[i+1]["x"] - curr["x"]) * dx < 0 )
                    
                    if prev_flat or next_flat or prev_dir_change or next_dir_change:
                        is_slam = True
                
                if is_slam:
                    slam_dur = min(dt, 35.0)
                    curr_copy = curr.copy()
                    curr_copy["offset"] = round(prev["offset"] + slam_dur, 6)
                    new_nodes.append(curr_copy)
                else:
                    curr["offset"] = round(curr["offset"], 6)
                    new_nodes.append(curr) 
            
            segment["nodes"] = new_nodes
            if "bpm" in segment: del segment["bpm"]

    general["audioOffset"] = 0

    output_data = {
        "general": general,
        "hitObjects": hit_objects,
        "lasers": lasers
    }

    json_filename = map_difficulty_to_filename(general["difficulty"])
    json_path = os.path.join(dest_folder, json_filename)
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2)

    return json_filename, general.get("audioFilename"), general.get("jacketFilename")


def run_batch_conversion():
    base_dir = os.getcwd()
    export_dir = os.path.join(base_dir, "Export")
    
    print(f"Starting batch conversion in: {base_dir}")
    os.makedirs(export_dir, exist_ok=True)
    folders_processed = 0

    for item in os.listdir(base_dir):
        source_folder = os.path.join(base_dir, item)
        if not os.path.isdir(source_folder) or item == "Export":
            continue

        ksh_files = [f for f in os.listdir(source_folder) if f.lower().endswith('.ksh')]
        if not ksh_files: continue

        folders_processed += 1
        print(f"\nProcessing folder: '{item}'")
        dest_folder = os.path.join(export_dir, item)
        os.makedirs(dest_folder, exist_ok=True)
        media_files_to_copy = set()

        for ksh_file in ksh_files:
            ksh_path = os.path.join(source_folder, ksh_file)
            try:
                json_filename, audio_file, jacket_file = convert_ksh_to_json(ksh_path, dest_folder)
                print(f"  [+] Converted: {ksh_file} -> {json_filename}")
                if audio_file: media_files_to_copy.add(audio_file)
                if jacket_file: media_files_to_copy.add(jacket_file)
            except Exception as e:
                print(f"  [!] Error converting {ksh_file}: {e}")

        for media_file in media_files_to_copy:
            src_media = os.path.join(source_folder, media_file)
            dst_media = os.path.join(dest_folder, media_file)
            if os.path.exists(src_media):
                if not os.path.exists(dst_media):
                    shutil.copy2(src_media, dst_media)
                    print(f"  [>] Copied explicitly requested media: {media_file}")
                else:
                    print(f"  [=] Media already exists: {media_file}")
            else:
                print(f"  [!] Warning: Chart requested '{media_file}', but it was not found.")

    print(f"\nBatch processing complete! Processed {folders_processed} folder(s).")

if __name__ == "__main__":
    run_batch_conversion()