# Overview 

This tutorial demonstrates using  Hazelcast Enterprise 5.5 to build an image 
search system.  The solution uses the CLIP sentence transformer 
(https://huggingface.co/sentence-transformers/clip-ViT-B-32) to map images 
and text onto a shared vector 512-dimensional vector space. This solution 
demonstrates the following things.
- a Hazelcast Pipeline that consumes unstructured data (images), computes 
embeddings using Python, and stores them as vectors in a Hazelcast Enterprise 
vector collection.
- a Jupyter notebook that implements text based image searching using 
the Hazelcast Python client


The pipeline has the following high level components:
1. Directory Watcher detects the arrival of new images and creates an event 
containing the name of the new image.
2. A mapUsingPython stage in which images are retrieved and converted into 
vectors using the previously mentioned CLIP sentence transformer.
3. An Sink which stores the image vectors, along with their URLs, in
a Hazelcast vector collection.

See the blueprint below.

![BluePrint](resources/blueprint.png)

# Prerequisites

- Docker Desktop
- An Internet Connection

This demonstration needs to download quite a few python packages, docker 
images, etc..

# Setup 
The model we will be using to perform embedding is almost 500M.  To speed 
up everything that uses the model, we can download it ahead of time.

Run `docker compose run download-model`

Verify that the `models` folder of the project has been populated.

# Obtain Images

Download https://data.caltech.edu/records/mzrjq-6wc02/files/caltech-101.zip  and place it in
the `images` folder.  Unpack it, then unpack `caltech-101/101_ObjectCategories.tar.gz`.  You should end up with multiple directories underneath `images/caltech-101/101_ObjectCategories`, each containing multiple images.


# Issues

No visibility of vector collections in man center.

The String/String interface on mapUsingPython

mapUsingPython fails to initialize, probably because of flock

# Known Issues

1. The DirectoryWatcher provided in this project does not detect file deletes.  Currently it will only issue 
   events when files are added or updated.
2. Delete detection is not working.  If an image is removed from `www`, 
   it will not be removed from the vector collection.
3. If too many images are dumped into `www` at the same time, the pipeline will break with a 
  'grpc max message size exceeded' message.
4. Deploying the pipeline can take 5-10 minutes depending on your internet connection.  This is due to the need 
   to download many python packages.  