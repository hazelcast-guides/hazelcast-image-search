import os
import os.path
import shutil

base = 'caltech-101/101_ObjectCategories'
for category in os.listdir(base):
    category_dir = os.path.join(base, category)
    if os.path.isdir(category_dir):
        os.mkdir(category)
        for n in range(1, 21):
            suffix = f'{n:04d}'
            shutil.copy(
                os.path.join(category_dir, f'image_{suffix}.jpg'),
                os.path.join(category, f'{category}_{suffix}.jpg'))

