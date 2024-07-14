import os
import os.path
import shutil

base = 'caltech-101/101_ObjectCategories'
for category in os.listdir(base):
    category_dir = os.path.join(base, category)
    if os.path.isdir(category_dir):
        for n in range(1, 21):
            suffix = f'{n:04d}'
            shutil.copy(os.path.join(category_dir, f'image_{suffix}.jpg'), f'{category}_{suffix}.jpg')

