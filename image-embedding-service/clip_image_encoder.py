import base64
import io
import json

import PIL.Image
import sentence_transformers

encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')
print("downloaded and initialized encoder")


#
# The input is expected to be a list of json encoded strings that look as follows
#   { "metadata": any-json-content, "content": "base64 encoded image bytes" }
#
# The output will be a list in the following form:
#   {"metadata": any-json-content, "vector", [json, encoded, list, of, numbers]}
#
# Note: metadata is not used, it is just passed through
#
def transform_list(image_json: list[str]) -> list[str]:
    inputs = [json.loads(s) for s in image_json]
    metadata_list = [item['metadata'] for item in inputs]

    # decode base64 strings into byte streams
    byte_streams = [io.BytesIO(base64.b64decode(item['content'])) for item in inputs]

    # load images from byte streams
    images = [PIL.Image.open(b) for b in byte_streams]

    # perform encoding - returns nx512 numpy array where n is the number of inputs (images)
    embeddings = encoder.encode(images)

    # close byte streams
    for bs in byte_streams:
        bs.close()

    # embeddings is a 2d ndarray. Create a list of float arrays from it
    floatarrays = [embeddings[i].tolist() for i in range(len(embeddings))]

    # create a list of result objects that combine the result vectors and the corresponding metadata
    results = [{'metadata': m, 'vector': v} for m, v in zip(metadata_list, floatarrays)]

    # encode each float array as json and return a list of strings
    return [json.dumps(result) for result in results]
