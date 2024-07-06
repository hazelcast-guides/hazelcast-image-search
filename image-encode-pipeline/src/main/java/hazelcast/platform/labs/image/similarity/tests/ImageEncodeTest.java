package hazelcast.platform.labs.image.similarity.tests;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.TranslateException;

import java.io.IOException;
import java.nio.file.Path;

public class ImageEncodeTest {
    public static void main(String []args){
        Criteria<NDList, NDList> criteria =
                Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelUrls("https://resources.djl.ai/demo/pytorch/clip.zip")
                        .optTranslator(new NoopTranslator())
                        .optEngine("PyTorch")
                        .optDevice(Device.cpu()) // torchscript model only support CPU
                        .build();
        try ( ZooModel<NDList, NDList> clip = criteria.loadModel();
              Predictor<Image, NDArray> imageFeatureExtractor = clip.newPredictor(new ImageTranslator());
              Predictor<String, NDArray> textFeatureExtractor = clip.newPredictor(new TextTranslator());
              NDManager ndManager = NDManager.newBaseManager())
        {
            String image1Path = "/Users/rmay/Documents/projects/hazelcast-image-search/images/caltech-101/101_ObjectCategories/dragonfly/image_0003.jpg";
            String image2Path = "/Users/rmay/Documents/projects/hazelcast-image-search/images/caltech-101/101_ObjectCategories/schooner/image_0044.jpg";
            String [] comparisons =
                    new String []{ "ship", "boat", "dragonfly", "dragon", "insect", "wings", "helicopter", "bicycle","happy child", "clouds", "dragonfly and clouds", "blue", "white sailboat with blue sails", "blue sailboat with white sails", "ocean", "hot dog"};

            compare(image1Path, comparisons, imageFeatureExtractor, textFeatureExtractor);
            compare(image2Path, comparisons, imageFeatureExtractor, textFeatureExtractor);
        } catch (IOException | ModelNotFoundException | MalformedModelException | TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    public static void compare(String imgPath,
                               String []comparisons,
                               Predictor<Image, NDArray> imageFeatureExtractor,
                               Predictor<String, NDArray> textFeatureExtractor)
            throws IOException, TranslateException {
        System.out.println("Image: " + imgPath);
        Image image1 = ImageFactory.getInstance().fromFile(Path.of(imgPath));

        NDArray imageVector = imageFeatureExtractor.predict(image1);
        NDArray imageVectorTranspose = imageVector.transpose();

        NDArray imageVectorNorm = imageVector.pow(2).sum().sqrt();

        for(String comparison: comparisons){
            NDArray textVector = textFeatureExtractor.predict(comparison);

            // compute cosine similarity as a dot b / norm(a) norm(b)
            NDArray textVectorNorm = textVector.pow(2).sum().sqrt();

            NDArray similarity = textVector.dot(imageVectorTranspose).div(imageVectorNorm.mul(textVectorNorm));

            System.out.println("similarity to \"" + comparison + "\": " + similarity);
        }
    }
}
