import sentence_transformers

print('started')
encoder = sentence_transformers.SentenceTransformer('clip-ViT-B-32')
print('downloaded')
encoder.save('../models/clip-ViT-B-32')
print('saved')
