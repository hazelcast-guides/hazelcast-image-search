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
    results = []

    # could use list comprehensions throughout but not doing it because I need
    # to capture exceptions on individual items
    for j in image_json:
        try:
            item = json.loads(j)
            metadata = item['metadata']

            # decode base64 string into byte stream
            byte_stream = io.BytesIO(base64.b64decode(item['content']))

            # load image from byte streams
            image = PIL.Image.open(byte_stream)

            # perform encoding - returns numpy array with the embedding for this image
            embedding = encoder.encode(image)

            # close byte streams
            byte_stream.close()

            # Create a float arrays from it
            floatarray = embedding.tolist()
            results.append({'metadata': metadata, 'vector': floatarray})
        except Exception as x:
            results.append({'exception': str(x)})

    # encode each float array as json and return a list of strings
    return [json.dumps(result) for result in results]
