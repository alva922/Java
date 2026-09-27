import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class PngToGif {

    // Folder containing all PNG frames
    private static final String INPUT_FOLDER =
            "C:\\JavaProjects\\Animation\\ball";

    // Output GIF file
    private static final String OUTPUT_GIF =
            INPUT_FOLDER + "\\bouncing_ball.gif";

    // Delay between frames in milliseconds
    private static final int FRAME_DELAY = 100;

    public static void main(String[] args) {

        try {
            File folder = new File(INPUT_FOLDER);

            if (!folder.exists() || !folder.isDirectory()) {
                System.err.println("Folder not found: " + INPUT_FOLDER);
                return;
            }

            File[] pngFiles = folder.listFiles(
                    (dir, name) ->
                            name.toLowerCase().endsWith(".png"));

            if (pngFiles == null || pngFiles.length == 0) {
                System.err.println("No PNG files found.");
                return;
            }

            // Sort files alphabetically:
            // red_ball_frame_01.png
            // red_ball_frame_02.png
            // ...
            Arrays.sort(pngFiles);

            ImageOutputStream output =
                    new FileImageOutputStream(
                            new File(OUTPUT_GIF));

            GifSequenceWriter writer =
                    new GifSequenceWriter(
                            output,
                            BufferedImage.TYPE_INT_ARGB,
                            FRAME_DELAY,
                            true);

            for (File pngFile : pngFiles) {

                System.out.println(
                        "Adding frame: " + pngFile.getName());

                BufferedImage image =
                        ImageIO.read(pngFile);

                writer.writeToSequence(image);
            }

            writer.close();
            output.close();

            System.out.println();
            System.out.println("GIF created successfully:");
            System.out.println(OUTPUT_GIF);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class GifSequenceWriter {

    private final ImageWriter gifWriter;
    private final ImageWriteParam imageWriteParam;
    private final IIOMetadata imageMetaData;

    public GifSequenceWriter(
            ImageOutputStream outputStream,
            int imageType,
            int delay,
            boolean loopContinuously)
            throws IOException {

        gifWriter = getWriter();
        imageWriteParam = gifWriter.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier =
                ImageTypeSpecifier.createFromBufferedImageType(
                        imageType);

        imageMetaData =
                gifWriter.getDefaultImageMetadata(
                        imageTypeSpecifier,
                        imageWriteParam);

        String metaFormatName =
                imageMetaData.getNativeMetadataFormatName();

        IIOMetadataNode root =
                (IIOMetadataNode) imageMetaData.getAsTree(
                        metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode =
                getNode(root, "GraphicControlExtension");

        graphicsControlExtensionNode.setAttribute(
                "disposalMethod",
                "none");

        graphicsControlExtensionNode.setAttribute(
                "userInputFlag",
                "FALSE");

        graphicsControlExtensionNode.setAttribute(
                "transparentColorFlag",
                "FALSE");

        graphicsControlExtensionNode.setAttribute(
                "delayTime",
                Integer.toString(delay / 10));

        graphicsControlExtensionNode.setAttribute(
                "transparentColorIndex",
                "0");

        IIOMetadataNode appExtensionsNode =
                getNode(root, "ApplicationExtensions");

        IIOMetadataNode appNode =
                new IIOMetadataNode(
                        "ApplicationExtension");

        appNode.setAttribute(
                "applicationID",
                "NETSCAPE");

        appNode.setAttribute(
                "authenticationCode",
                "2.0");

        int loop =
                loopContinuously ? 0 : 1;

        appNode.setUserObject(
                new byte[]{
                        0x1,
                        (byte) (loop & 0xFF),
                        (byte) ((loop >> 8) & 0xFF)
                });

        appExtensionsNode.appendChild(appNode);

        imageMetaData.setFromTree(
                metaFormatName,
                root);

        gifWriter.setOutput(outputStream);
        gifWriter.prepareWriteSequence(null);
    }

    public void writeToSequence(RenderedImage image)
            throws IOException {

        gifWriter.writeToSequence(
                new IIOImage(
                        image,
                        null,
                        imageMetaData),
                imageWriteParam);
    }

    public void close() throws IOException {
        gifWriter.endWriteSequence();
    }

    private static ImageWriter getWriter() {

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersBySuffix("gif");

        if (!writers.hasNext()) {
            throw new IllegalStateException(
                    "No GIF writer found.");
        }

        return writers.next();
    }

    private static IIOMetadataNode getNode(
            IIOMetadataNode rootNode,
            String nodeName) {

        int nNodes =
                rootNode.getLength();

        for (int i = 0; i < nNodes; i++) {

            if (rootNode.item(i)
                    .getNodeName()
                    .equalsIgnoreCase(nodeName)) {

                return (IIOMetadataNode)
                        rootNode.item(i);
            }
        }

        IIOMetadataNode node =
                new IIOMetadataNode(nodeName);

        rootNode.appendChild(node);

        return node;
    }
}