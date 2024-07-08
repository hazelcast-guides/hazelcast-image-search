import base64
import io
import json

import PIL.Image
import sentence_transformers

encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')
print("downloaded and initialized encoder")


def transform_list(image_bytes: list[str]) -> list[str]:
    # decode base64 strings into byte streams
    byte_streams = [io.BytesIO(base64.b64decode(x)) for x in image_bytes]

    # load images from byte streams
    images = [PIL.Image.open(b) for b in byte_streams]

    # perform encoding - returns nx512 numpy array where n is the number of inputs (images)
    embeddings = encoder.encode(images)

    # close byte streams
    for bs in byte_streams:
        bs.close()

    # turn the responses into a list of float arrays
    floatarrays = [embeddings[i].tolist() for i in range(len(embeddings))]

    # encode each float array as json and return a list of strings
    return [json.dumps(fa) for fa in floatarrays]
