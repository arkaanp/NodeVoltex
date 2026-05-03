import json

def convert_osu_to_json(input_file, output_file):
    hit_objects = []
    
    # Map osu!mania x-coordinates to lanes
    lane_mapping = {
        64: 1,
        192: 2,
        320: 3,
        448: 4
    }

    with open(input_file, 'r') as file:
        for line in file:
            line = line.strip()
            # Skip empty lines or headers
            if not line or line.startswith('['):
                continue
            
            parts = line.split(',')
            if len(parts) < 6:
                continue
                
            x_coord = int(parts[0])
            start_time = int(parts[2])
            type_flag = int(parts[3])
            
            # Determine lane
            lane = lane_mapping.get(x_coord)
            if not lane:
                continue # Ignore unrecognized lanes
                
            if type_flag == 128:
                # It's a HOLD note
                extras = parts[5].split(':')
                end_time = int(extras[0])
                hit_objects.append({
                    "lane": lane,
                    "startTime": start_time,
                    "type": "HOLD",
                    "endTime": end_time
                })
            elif type_flag == 1:
                # It's a TAP note
                hit_objects.append({
                    "lane": lane,
                    "startTime": start_time,
                    "type": "TAP"
                })

    # Wrap as JSON
    output = {"hitObjects": hit_objects}
    
    # Write directly to the output file
    with open(output_file, 'w') as outfile:
        json.dump(output, outfile, indent=2)
        
    print(f"Successfully converted! Saved to {output_file}")

# Use the function: it reads 'map.txt' and creates 'map.json'
convert_osu_to_json('map.txt', 'map.json')