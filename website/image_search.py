import hazelcast
from hazelcast.vector import Vector, VectorType
import sentence_transformers
from scipy.spatial.distance import cosine

encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')
print("downloaded and initialized encoder")

query = "jet engine"
query_vector = encoder.encode(query).tolist()
hz_query = Vector(name= 'semantic-search',type=VectorType.DENSE, vector=query_vector)
print("query encoded")


hz = hazelcast.HazelcastClient(
    cluster_members=[
        "localhost:5701"
    ],
    cluster_name="dev",
)
print("connected to Hazelcast")

vc = hz.get_vector_collection("images").blocking()
results = vc.search_near_vector(hz_query, include_value=True, limit=3)
for result in results:
    print(f'score: {result.score} value: {result.value}')

