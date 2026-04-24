import json

def convert_osu_to_json(file_path):
    hit_objects = []
    
    # Map osu!mania x-coordinates to lanes
    lane_mapping = {
        64: 1,
        192: 2,
        320: 3,
        448: 4
    }

    with open(file_path, 'r') as file:
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

    # Wrap and print as JSON
    output = {"hitObjects": hit_objects}
    print(json.dumps(output, indent=2))

# Use the function (make sure map.txt is in the same folder)
convert_osu_to_json('map.txt')