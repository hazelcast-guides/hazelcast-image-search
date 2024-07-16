# Overview 

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