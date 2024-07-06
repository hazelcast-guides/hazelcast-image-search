package hazelcast.platform.labs.image.similarity;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/*
 * Note: The first time this is instantiated, it may take some time because it will need to
 *       download the pretrained CLIP model.
 */
public class ImageEncoder implements AutoCloseable, NoBatchifyTranslator<byte [], float []> {

    private final ZooModel<NDList, NDList> clipModel;
    Predictor<byte [], float[]> imageFeatureExtractor;

    public static ImageEncoder newInstance(){
        try {
            return new ImageEncoder();
        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ImageEncoder() throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria<NDList, NDList> criteria =
                Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelUrls("https://resources.djl.ai/demo/pytorch/clip.zip")
                        .optTranslator(new NoopTranslator())
                        .optEngine("PyTorch")
                        .optDevice(Device.cpu()) // torchscript model only support CPU
                        .build();

        this.clipModel = criteria.loadModel();
        this.imageFeatureExtractor = clipModel.newPredictor(this);
    }

    public float []encodeImage(byte []imageBytes){
        try {
            return imageFeatureExtractor.predict(imageBytes);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        clipModel.close();
    }

    @Override
    public float[] processOutput(TranslatorContext translatorContext, NDList ndList) {
        return ndList.singletonOrThrow().toFloatArray();
    }

    @Override
    public NDList processInput(TranslatorContext translatorContext, byte[] bytes) throws Exception {
        Image image = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(bytes));
        NDArray imageData = image.toNDArray(translatorContext.getNDManager(), Image.Flag.COLOR);

        // resize the image so the smallest dimension is 224 then crop out the central 224x224 square
        float percent = 224f / Math.min(image.getWidth(), image.getHeight());
        int resizedWidth = Math.round(image.getWidth() * percent);
        int resizedHeight = Math.round(image.getHeight() * percent);
        imageData = NDImageUtils.resize(imageData, resizedWidth, resizedHeight, Image.Interpolation.BICUBIC);
        imageData = NDImageUtils.centerCrop(imageData, 224, 224);

        // prepare for processing by the CLIP model
        imageData = NDImageUtils.toTensor(imageData);
        NDArray placeholder = translatorContext.getNDManager().create("");
        placeholder.setName("module_method:get_image_features");
        return new NDList(imageData.expandDims(0), placeholder);
    }
}
