FROM python:3

RUN pip install sentence-transformers jupyter pytest

WORKDIR /root

RUN git clone https://github.com/hazelcast/hazelcast-python-client.git

CMD cd /project/ui && export PYTHONPATH=/root/hazelcast-python-client \
    &&  jupyter notebook --allow-root --port=8888 --ip=* --no-browser
