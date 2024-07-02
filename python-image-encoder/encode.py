import time

from scipy.spatial.distance import cosine
import sentence_transformers
import PIL

if __name__ == '__main__':
    t0 = time.time()
    test_image = PIL.Image.open('/Users/rmay/Documents/projects/hazelcast-image-search/images/caltech-101/101_ObjectCategories/dragonfly/image_0003.jpg')
    t1 = time.time()
    print(f'Loaded image in {t1-t0}s')
    encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')
    t2 = time.time()
    print(f'Loaded model in {t2 - t1}s')
    v1 = encoder.encode(test_image)
    print(f'type of v1: {type(v1)} v1={v1}')

    comparisons = ['dragonfly', 'dragon', 'insect', 'wings', 'helicopter', 'bicycle','happy child', 'clouds', 'dragonfly and clouds', 'blue']
    for thing in comparisons:
        v_thing = encoder.encode(thing)
        d = cosine(v1, v_thing)
        print(f'distance from "{thing}": {d}')

