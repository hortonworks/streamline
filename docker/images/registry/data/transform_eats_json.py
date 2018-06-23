#!/usr/bin/env python

import json
import sys
from pprint import pprint
file = sys.argv[1]
output_file = open(sys.argv[2], 'w')

with open(file) as f:
    for line in f:
        try:
            data = json.loads(line)
            msg_dict = data['msg']
            result = {}
            for key in msg_dict:
                if "at_" in key and not msg_dict[key]:
                    result[key] = 0
                else:
                    result[key] = msg_dict[key]
            json.dump(result, output_file, ensure_ascii=False)
            output_file.write("\n")
        except:
            continue
output_file.close()

