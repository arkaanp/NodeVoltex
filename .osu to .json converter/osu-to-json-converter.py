import json
import sys
import os
import shutil

def map_difficulty_to_filename(diff_str):
    """
    Maps osu! difficulty names to the strict 4-tier naming scheme.
    Default fallback is 'mxm.json' if it can't find a lower-tier keyword.
    """
    diff_str = diff_str.lower().strip()
    
    if any(k in diff_str for k in ['easy', 'novice', 'light', 'beginner', 'basic']):
        return 'nov.json'
    elif any(k in diff_str for k in ['normal', 'advanced', 'challenge', 'hard']):
        return 'adv.json'
    elif any(k in diff_str for k in ['hyper', 'insane', 'exhaust', 'expert']):
        return 'exh.json'
    
    # Fallback for extra, extreme, infinite, gravity, exceed, vivid, etc.
    return 'mxm.json'

def convert_osu_to_json(osu_path, dest_folder):
    with open(osu_path, 'r', encoding='utf-8-sig', errors='replace') as f:
        lines = [line.strip() for line in f.readlines()]

    general = {
        "title": "", "artist": "", "mapper": "", "illustrator": "",
        "difficulty": "mxm", "level": 1, "baseBpm": 120.0,
        "audioFilename": "audio.mp3", "audioVolume": 100, "audioOffset": 0,
        "jacketFilename": "", "background": "", "layer": "",
        "previewOffset": 0, "previewLength": 0, "defaultFilterType": "peak",
        "filterGain": 50, "slamAutoVolume": 0, "slamVolume": 50
    }

    hit_objects = []
    
    # Empty lasers as requested
    lasers = {"left": [], "right": []}

    current_section = ""
    timing_points = []
    keys = 4

    # 1. Parse the .osu file lines
    for line in lines:
        if not line or line.startswith('//'):
            continue
            
        if line.startswith('[') and line.endswith(']'):
            current_section = line[1:-1]
            continue

        if current_section == "General":
            if line.startswith("AudioFilename:"):
                general["audioFilename"] = line.split(":", 1)[1].strip()
            elif line.startswith("PreviewTime:"):
                val = int(line.split(":", 1)[1].strip())
                general["previewOffset"] = max(0, val)
                
        elif current_section == "Metadata":
            if line.startswith("Title:"):
                general["title"] = line.split(":", 1)[1].strip()
            elif line.startswith("Artist:"):
                general["artist"] = line.split(":", 1)[1].strip()
            elif line.startswith("Creator:"):
                general["mapper"] = line.split(":", 1)[1].strip()
            elif line.startswith("Version:"):
                general["difficulty"] = line.split(":", 1)[1].strip()
                
        elif current_section == "Difficulty":
            if line.startswith("OverallDifficulty:"):
                general["level"] = int(float(line.split(":", 1)[1].strip()))
            elif line.startswith("CircleSize:"):
                keys = int(line.split(":", 1)[1].strip())
                
        elif current_section == "TimingPoints":
            parts = line.split(',')
            if len(parts) >= 2:
                beat_length = float(parts[1])
                # Uninherited timing point (positive beatLength) defines base BPM
                if beat_length > 0:
                    timing_points.append(60000.0 / beat_length)
                    
        elif current_section == "HitObjects":
            parts = line.split(',')
            if len(parts) >= 5:
                x = int(parts[0])
                start_time = int(parts[2])
                type_flag = int(parts[3])
                
                # Standard osu!mania column calculation
                column_idx = int(x * keys / 512)
                
                # Lane Routing based on Key Count
                mapped_lane = None
                if keys == 4:
                    # 4K: 1->1, 2->2, 3->3, 4->4
                    mapping_4k = {0: 1, 1: 2, 2: 3, 3: 4}
                    mapped_lane = mapping_4k.get(column_idx, 1)
                elif keys == 6:
                    # 6K: Outer lanes (0, 5) -> FX (5, 6) | Inner lanes (1, 2, 3, 4) -> BT (1, 2, 3, 4)
                    mapping_6k = {0: 5, 1: 1, 2: 2, 3: 3, 4: 4, 5: 6}
                    mapped_lane = mapping_6k.get(column_idx, 1)
                else:
                    continue # Skip unsupported key counts for now

                # Bitmask 128 determines if it's a hold note in mania
                is_hold = (type_flag & 128) != 0
                
                if is_hold and len(parts) >= 6:
                    # The end time is the first value in the colon-separated objectParams
                    end_time = int(parts[5].split(':')[0])
                    hit_objects.append({
                        "lane": mapped_lane,
                        "startTime": start_time,
                        "type": "HOLD",
                        "endTime": end_time
                    })
                else:
                    hit_objects.append({
                        "lane": mapped_lane,
                        "startTime": start_time,
                        "type": "TAP"
                    })

    # Set base BPM from the first timing point
    if timing_points:
        general["baseBpm"] = round(timing_points[0], 2)

    # Sort objects chronologically 
    hit_objects.sort(key=lambda x: x["startTime"])

    output_data = {
        "general": general,
        "hitObjects": hit_objects,
        "lasers": lasers
    }

    # Format the file string dynamically 
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

    # Iterate through all folders in the current directory
    for item in os.listdir(base_dir):
        source_folder = os.path.join(base_dir, item)
        if not os.path.isdir(source_folder) or item == "Export":
            continue

        osu_files = [f for f in os.listdir(source_folder) if f.lower().endswith('.osu')]
        if not osu_files: 
            continue

        folders_processed += 1
        print(f"\nProcessing folder: '{item}'")
        dest_folder = os.path.join(export_dir, item)
        os.makedirs(dest_folder, exist_ok=True)
        
        media_files_to_copy = set()

        for osu_file in osu_files:
            osu_path = os.path.join(source_folder, osu_file)
            try:
                json_filename, audio_file, jacket_file = convert_osu_to_json(osu_path, dest_folder)
                print(f"  [+] Converted: {osu_file} -> {json_filename}")
                if audio_file: media_files_to_copy.add(audio_file)
                if jacket_file: media_files_to_copy.add(jacket_file)
            except Exception as e:
                print(f"  [!] Error converting {osu_file}: {e}")

        # Handle transferring Audio files identically to your referenced script
        for media_file in media_files_to_copy:
            if not media_file: continue
            
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