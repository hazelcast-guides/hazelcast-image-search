import base64
import json
import clip_image_encoder
import time
from scipy.spatial.distance import cosine
import sentence_transformers

if __name__ == '__main__':
    t0 = time.time()
    test_images = ['/Users/rmay/Documents/projects/hazelcast-image-search/images/caltech-101/101_ObjectCategories/dragonfly/image_0003.jpg',
                   '/Users/rmay/Documents/projects/hazelcast-image-search/images/caltech-101/101_ObjectCategories/dragonfly/image_0002.jpg']

    test_image_bytes = []
    for fname in test_images:
        with open(fname,'rb') as f:
            test_image_bytes.append(base64.b64encode(f.read()).decode('ascii'))

    inputs = [{'metadata': fn, 'content': b} for fn,b in zip(test_images, test_image_bytes)]

    embeddings = clip_image_encoder.transform_list([json.dumps(input) for input in inputs])

    outputs = [json.loads(r) for r in embeddings]

    floatarrays = [j['vector'] for j in outputs]
    v1 = floatarrays[0]

    text_encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')

    comparisons = ['dragonfly', 'dragon', 'insect', 'wings', 'helicopter', 'bicycle','happy child', 'clouds', 'dragonfly and clouds', 'blue']
    for thing in comparisons:
        v_thing = text_encoder.encode(thing)
        d = cosine(v1, v_thing)
        print(f'distance from "{thing}": {d}')

