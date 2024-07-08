import json
import sys

test_1 = [22.1, 1, 0.123456789, 1.23456789e-3, 1e-9]
json.dump(test_1, fp=sys.stdout)
