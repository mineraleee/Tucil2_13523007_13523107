import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class QuadTreeNode implements Serializable{
    int x, y, width, height, color;
    boolean isLeaf;
    QuadTreeNode[] children;

    public QuadTreeNode(int x, int y, int width, int height, int color, boolean isLeaf) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.isLeaf = isLeaf;
        this.children = isLeaf ? null : new QuadTreeNode[4];
    }
}

public class QuadTreeCompression {
    private static int method;
    private static double threshold;
    private static int minBlockSize;
    private static long startTime, thresholdTime =0;
    private static int totalNodes = 0;
    private static int maxDepth = 0;
    private static BufferedImage currentFrame;
    private static List<BufferedImage> frames = new ArrayList<>();
    private static double targetCompression;
    private static boolean isMinBlock;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        BufferedImage img = null;
        String imagePath;

        // Loop untuk validasi gambar
        while (true) {
            System.out.print("Masukkan nama gambar (beserta ekstensi .jpg/.png): ");
            String imageName = scanner.nextLine();
            imagePath = "../test/source/" + imageName;

            File imageFile = new File(imagePath);
    
            // Cek apakah file ada
            if (!imageFile.exists() || imageFile.isDirectory()) {
                System.out.println("File tidak ditemukan. Silakan masukkan nama gambar yang benar.");
                continue;
            }

            try {
                img = ImageIO.read(imageFile);

                // Jika gambar tidak valid
                if (img == null) {
                    System.out.println("Format gambar tidak didukung atau file rusak. Silakan masukkan gambar lain.");
                    continue;
                }

                // Jika gambar valid, keluar dari loop
                break;

            } catch (IOException e) {
                System.out.println("Terjadi kesalahan saat membaca gambar: " + e.getMessage());
            }
        }
        
        File inputFile = new File (imagePath);
        long originalSize = inputFile.length();
        
        System.out.print("Pilih metode error (1: Variansi, 2: MAD, 3: Max Pixel Difference, 4: Entropy, 5: SSIM): ");
        method = scanner.nextInt();
       
       
        System.out.print("Masukkan target persentase kompresi (beri nilai 0 jika ingin menonaktifkan): ");
        targetCompression = scanner.nextDouble();
        if (targetCompression == 0){
            isMinBlock = false;
            System.out.print("Masukkan threshold: ");
            threshold = scanner.nextDouble();
            System.out.print("Masukkan ukuran blok minimum: ");
            minBlockSize = scanner.nextInt();
        } else{
            isMinBlock = true;
            long thresholdStart = System.nanoTime();
            threshold = findBestThreshold (img, originalSize);
            thresholdTime = System.nanoTime() - thresholdStart;
        }
        System.out.print("Masukkan nama gambar hasil (beserta ekstensinya .jpg/.jpeg./.png): ");
        String outputName = scanner.next();
        System.out.print("Masukkan nama file GIF hasil (akhiri dengan .gif): ");
        String gifName = scanner.next();
        String outputFolder = "../test/result/";
        String outputPath = outputFolder + outputName;
        String gifPath = outputFolder + gifName;

        File resultDir = new File(outputFolder);
        if (!resultDir.exists()) {
            if (resultDir.mkdirs()) {
                System.out.println("Folder 'result' berhasil dibuat.");
            } else {
                System.out.println("Gagal membuat folder 'result'. Pastikan program memiliki izin menulis.");
                return;
            }
        }

        startTime = System.nanoTime();
        currentFrame = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = currentFrame.createGraphics();
        g.setColor(new Color(averageColor(img, 0, 0, img.getWidth(), img.getHeight())));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        //frames.add(copyOf(currentFrame));

        //QuadTreeNode root = buildQuadTree(img, 0, 0, img.getWidth(), img.getHeight(), 0);
        QuadTreeNode root = buildQuadTreeBFS(img);
        saveGif(gifPath, frames, 500); // delay per frame: 200ms
        long executionTime = System.nanoTime() - startTime;
        
        BufferedImage compressedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        drawQuadTree(compressedImage, root);
        ImageIO.write(compressedImage, "png", new File(outputPath));

        int compressedSize = getSerializedSize(root); 
        double compressionPercentage = 100.0 - ((double) compressedSize / originalSize * 100);

        System.out.println("Waktu eksekusi: " + (executionTime / 1e6) + " ms");
        System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
        System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
        System.out.println("Persentase kompresi: " + String.format("%.2f", compressionPercentage) + "%");
        System.out.println("Kedalaman maksimal pohon: " + maxDepth);
        System.out.println("Total simpul pohon: " + totalNodes);
        System.out.println("Gambar hasil disimpan di: " + outputPath);
    }

    private static BufferedImage copyOf(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return copy;
    }            

    private static QuadTreeNode buildQuadTreeBFS(BufferedImage img) {
        Queue<QuadTreeNode> queue = new LinkedList<>();
        QuadTreeNode root = new QuadTreeNode(0, 0, img.getWidth(), img.getHeight(),
                averageColor(img, 0, 0, img.getWidth(), img.getHeight()), false);
        queue.offer(root);
    
        currentFrame = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        frames.clear();
    
        // Frame awal (satu warna)
        Graphics2D gInit = currentFrame.createGraphics();
        gInit.setColor(new Color(root.color));
        gInit.fillRect(0, 0, img.getWidth(), img.getHeight());
        gInit.dispose();
        frames.add(copyOf(currentFrame));
    
        List<QuadTreeNode> currentLeaves = new ArrayList<>();
    
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<QuadTreeNode> newChildren = new ArrayList<>();
    
            for (int i = 0; i < size; i++) {
                QuadTreeNode node = queue.poll();
                double err = computeError(img, node.x, node.y, node.width, node.height);
                int halfWidth = node.width / 2;
                int halfHeight = node.height / 2;

                if((isMinBlock && (halfWidth*halfHeight < minBlockSize || err < threshold)) || !isMinBlock && (err < threshold)){
                    node.isLeaf = true;
                    totalNodes++;
                    currentLeaves.add(node);
                }else {
                    QuadTreeNode[] children = new QuadTreeNode[4];
                    children[0] = new QuadTreeNode(node.x, node.y, halfWidth, halfHeight,
                            averageColor(img, node.x, node.y, halfWidth, halfHeight), false);
                    children[1] = new QuadTreeNode(node.x + halfWidth, node.y, node.width - halfWidth, halfHeight,
                            averageColor(img, node.x + halfWidth, node.y, node.width - halfWidth, halfHeight), false);
                    children[2] = new QuadTreeNode(node.x, node.y + halfHeight, halfWidth, node.height - halfHeight,
                            averageColor(img, node.x, node.y + halfHeight, halfWidth, node.height - halfHeight), false);
                    children[3] = new QuadTreeNode(node.x + halfWidth, node.y + halfHeight, node.width - halfWidth,
                            node.height - halfHeight,
                            averageColor(img, node.x + halfWidth, node.y + halfHeight, node.width - halfWidth,
                                    node.height - halfHeight),
                            false);
    
                    node.children = children;
                    Collections.addAll(newChildren, children);
                }
            }
            if (!newChildren.isEmpty()) {
                Graphics2D g = currentFrame.createGraphics();
                // gambar semua leaf
                for (QuadTreeNode leaf : currentLeaves) {
                    g.setColor(new Color(leaf.color));
                    g.fillRect(leaf.x, leaf.y, leaf.width, leaf.height);
                }
                // gambar semua children baru
                for (QuadTreeNode child : newChildren) {
                    g.setColor(new Color(child.color));
                    g.fillRect(child.x, child.y, child.width, child.height);
                }
                g.dispose();
                frames.add(copyOf(currentFrame));
                queue.addAll(newChildren);
            }
        }
        return root;
    }
    
    private static QuadTreeNode buildQuadTreeBFSwithThreshold(BufferedImage img, double thresholdVal) {
        Queue<QuadTreeNode> queue = new LinkedList<>();
        QuadTreeNode root = new QuadTreeNode(0, 0, img.getWidth(), img.getHeight(),
        averageColor(img, 0, 0, img.getWidth(), img.getHeight()), false);
        queue.offer(root);
    
        currentFrame = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        frames.clear();
    
        // Frame awal (satu warna)
        Graphics2D gInit = currentFrame.createGraphics();
        gInit.setColor(new Color(root.color));
        gInit.fillRect(0, 0, img.getWidth(), img.getHeight());
        gInit.dispose();
        frames.add(copyOf(currentFrame));
    
        List<QuadTreeNode> currentLeaves = new ArrayList<>();
    
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<QuadTreeNode> newChildren = new ArrayList<>();
    
            for (int i = 0; i < size; i++) {
                QuadTreeNode node = queue.poll();
                double err = computeError(img, node.x, node.y, node.width, node.height);
    
                if (err < thresholdVal) {
                    node.isLeaf = true;
                    totalNodes++;
                    currentLeaves.add(node);
    
                } else {
                    int halfWidth = node.width / 2;
                    int halfHeight = node.height / 2;
                    QuadTreeNode[] children = new QuadTreeNode[4];
                    children[0] = new QuadTreeNode(node.x, node.y, halfWidth, halfHeight,
                            averageColor(img, node.x, node.y, halfWidth, halfHeight), false);
                    children[1] = new QuadTreeNode(node.x + halfWidth, node.y, node.width - halfWidth, halfHeight,
                            averageColor(img, node.x + halfWidth, node.y, node.width - halfWidth, halfHeight), false);
                    children[2] = new QuadTreeNode(node.x, node.y + halfHeight, halfWidth, node.height - halfHeight,
                            averageColor(img, node.x, node.y + halfHeight, halfWidth, node.height - halfHeight), false);
                    children[3] = new QuadTreeNode(node.x + halfWidth, node.y + halfHeight, node.width - halfWidth,
                            node.height - halfHeight,
                            averageColor(img, node.x + halfWidth, node.y + halfHeight, node.width - halfWidth,
                                    node.height - halfHeight),
                            false);
    
                    node.children = children;
                    Collections.addAll(newChildren, children);
                }
            }
        }
        return root;
    }

    private static void saveGif(String path, List<BufferedImage> frames, int delayMs) throws IOException {
        ImageOutputStream output = new FileImageOutputStream(new File(path));
        GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, delayMs, true);
        int i = 1;
        for (BufferedImage frame : frames) {
            writer.writeToSequence(frame);
            //ImageIO.write(frame, "png", new File("frame_" + i + ".png"));
            i++;
        }
        writer.close();
        output.close();
    }

    private static void drawQuadTree(BufferedImage img, QuadTreeNode node) {
        if (node.isLeaf) {
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(node.color));
            g.fillRect(node.x, node.y, node.width, node.height);
            g.dispose();
        } else {
            for (QuadTreeNode child : node.children) {
                if (child != null) drawQuadTree(img, child);
            }
        }
    }


    private static double findBestThreshold(BufferedImage img, long originalSize) throws IOException {
        double low = 0;
        double high = 10;
        double bestCompressionDiff = Double.MAX_VALUE;
        double achievedCompression =0;
        int iteration = 0;

        // System.out.println("Target Compression: " + targetCompression);
    
        // Step 1: Perluas high jika kompresi masih negatif
        while (true) {
            QuadTreeNode root = buildQuadTreeBFSwithThreshold(img, high);
            int compressed = getSerializedSize(root);
            achievedCompression = 1.0 - ((double) compressed / originalSize);
            // System.out.printf("[EXTEND-HIGH] Threshold=%.2f | Compressed=%d | Achieved=%.4f\n", high, compressed, achievedCompression);
    
            if (achievedCompression >= targetCompression) break;
            high += 500;
        }
        double bestThreshold = high;
        double highest = high;
        double diff = Math.abs(achievedCompression - targetCompression);
    
        // Step 2: Binary search
        while (iteration <=5 && diff>0.001){
            // System.out.println("achieved found: " + achievedCompression);
            // System.out.println("target: " + targetCompression);
            // System.out.println("diff: " + diff);
            int count = 0;
            while (diff > 0.001 && count <=5) {
                double mid = (low + high) / 2;
                QuadTreeNode root = buildQuadTreeBFSwithThreshold(img, mid);
                int compressed = getSerializedSize(root);
                achievedCompression = 1.0 - ((double) compressed / originalSize);
        
                diff = Math.abs(achievedCompression - targetCompression);
                if (achievedCompression >= 0 && diff < bestCompressionDiff) {
                    bestCompressionDiff = diff;
                    bestThreshold = mid;
                }
        
                if (achievedCompression < targetCompression) {
                    low = mid;
                } else {
                    high = mid;
                }
                count++;
                // System.out.printf("Mid: %.2f | Achieved: %.4f | Diff: %.4f | Best: %.2f\n", mid, achievedCompression, diff, bestThreshold);
            }
            // System.out.println("-----");
            high = highest+500;
            low = 0;
            highest = high;
            iteration++;
        }
        // System.out.println("Best threshold found: " + bestThreshold);
        return bestThreshold;
    }

    private static double computeError(BufferedImage img, int x, int y, int width, int height) {
        switch (method) {
            case 1: return computeVariance(img, x, y, width, height);
            case 2: return computeMAD(img, x, y, width, height);
            case 3: return computeMaxDiff(img, x, y, width, height);
            case 4: return computeEntropy(img, x, y, width, height);
            case 5: return computeSSIM(img, x, y, width, height);
            default: return computeVariance(img, x, y, width, height);
        }
    }

    private static double computeVariance(BufferedImage img, int x, int y, int width, int height) {
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        // if (width * height > minBlockSize){
        //     return 0;
        // }
        // Hitung rata-rata tiap kanal warna
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                int color = img.getRGB(i, j);
                rSum += (color >> 16) & 0xFF;
                gSum += (color >> 8) & 0xFF;
                bSum += color & 0xFF;
                count++;
            }
        }

        if (count == 0) return 0; // Hindari pembagian dengan nol

        double rMean = (double) rSum / count;
        double gMean = (double) gSum / count;
        double bMean = (double) bSum / count;

        // Hitung varians tiap kanal warna
        double rVar = 0, gVar = 0, bVar = 0;

        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                int color = img.getRGB(i, j);
                double r = ((color >> 16) & 0xFF) - rMean;
                double g = ((color >> 8) & 0xFF) - gMean;
                double b = (color & 0xFF) - bMean;
                rVar += r * r;
                gVar += g * g;
                bVar += b * b;
            }
        }

        rVar /= count;
        gVar /= count;
        bVar /= count;

        // Hitung varians RGB gabungan
        return (rVar + gVar + bVar) / 3.0;
    }

    private static double computeMAD(BufferedImage img, int x, int y, int width, int height) {
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        // Hitung rata-rata tiap kanal warna
        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                rSum += (color >> 16) & 0xFF;
                gSum += (color >> 8) & 0xFF;
                bSum += color & 0xFF;
                count++;
            }
        }

        if (count == 0) return 0;

        double rMean = (double) rSum / count;
        double gMean = (double) gSum / count;
        double bMean = (double) bSum / count;

        // Hitung varians tiap kanal warna
        double madr = 0, madg = 0, madb = 0;

        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                double r = ((color >> 16) & 0xFF) - rMean;
                double g = ((color >> 8) & 0xFF) - gMean;
                double b = (color & 0xFF) - bMean;
                madr += Math.abs(r);
                madg += Math.abs(g);
                madb += Math.abs(b);
            }
        }

        madr /= count;
        madg /= count;
        madb /= count;

        // Hitung varians RGB gabungan
        return (madr + madg + madb) / 3.0;    
    }

    private static double computeMaxDiff(BufferedImage img, int x, int y, int width, int height) {
        int rMin = 255, gMin = 255, bMin = 255;
        int rMax = 0, gMax = 0, bMax = 0;

        // Hitung nilai min dan max tiap kanal warna dalam blok
        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                rMin = Math.min(rMin, r);
                gMin = Math.min(gMin, g);
                bMin = Math.min(bMin, b);

                rMax = Math.max(rMax, r);
                gMax = Math.max(gMax, g);
                bMax = Math.max(bMax, b);
            }
        }

        // Hitung selisih max-min tiap kanal
        int dR = rMax - rMin;
        int dG = gMax - gMin;
        int dB = bMax - bMin;

        // Hitung D_RGB sesuai rumus
        return (dR + dG + dB) / 3.0;
    }

    private static int averageColor(BufferedImage img, int x, int y, int width, int height) {
        long r = 0, g = 0, b = 0;
        int count = 0;
    
        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                r += (color >> 16) & 0xFF;
                g += (color >> 8) & 0xFF;
                b += color & 0xFF;
                count++; 
            }
        }
    
        if (count == 0) return 0; 
        r /= count;
        g /= count;
        b /= count;
    
        return (int) ((r << 16) | (g << 8) | b);
    }

    private static double computeEntropy(BufferedImage img, int x, int y, int width, int height) {
        int r = 0, g = 0, b = 0, count = 0;
        int[] rVal = new int[256];
        int[] gVal = new int[256];
        int[] bVal = new int[256];
        double[] rProb = new double[256];
        double[] gProb = new double[256];
        double[] bProb = new double[256];
        double rEntro = 0, gEntro = 0, bEntro = 0;

        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                r = (color >> 16) & 0xFF;
                g = (color >> 8) & 0xFF;
                b = color & 0xFF;
                rVal[r]++;
                gVal[g]++;
                bVal[b]++;
                count++; 
            }
        }
        if (count == 0) return 0;
        for (int i = 0; i < 256; i++) {
            rProb[i] = (double) rVal[i]/count;
            gProb[i] = (double) gVal[i]/count;
            bProb[i] = (double) bVal[i]/count;
        }

        for (int i = 0; i < 256; i++) {
            rEntro -= rProb[i]*(Math.log(rProb[i])/Math.log(2));
            gEntro -= gProb[i]*(Math.log(gProb[i])/Math.log(2));
            bEntro -= bProb[i]*(Math.log(bProb[i])/Math.log(2));
        }

        return (rEntro+gEntro+bEntro)/3;
    }

    private static double computeSSIM(BufferedImage img, int x, int y, int width, int height) {
        Color avgColor = new Color(averageColor(img, x, y, width, height));
        int ryMean = avgColor.getRed();
        int gyMean = avgColor.getGreen();
        int byMean = avgColor.getBlue();
        // C = (KL)^2, K = 0.01, K = 0.03
        double C1 = 6.5025, C2 = 58.5225;
        int rx = 0, gx = 0, bx = 0;
        int rxSum = 0, gxSum = 0, bxSum = 0;
        int count = 0;

        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                rx = (color >> 16) & 0xFF;
                gx = (color >> 8) & 0xFF;
                bx = color & 0xFF;
                rxSum += rx;
                gxSum += gx;
                bxSum += bx;
                count++; 
            }
        }
        
        if (count == 0) return 0; 
        double rxMean = (double) rxSum / count;
        double gxMean = (double) gxSum / count;
        double bxMean = (double) bxSum / count;

        double rxVar = 0, gxVar = 0, bxVar = 0;
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                int color = img.getRGB(i, j);
                double r = ((color >> 16) & 0xFF) - rxMean;
                double g = ((color >> 8) & 0xFF) - gxMean;
                double b = (color & 0xFF) - bxMean;
                rxVar += r * r;
                gxVar += g * g;
                bxVar += b * b;
            }
        }

        double rSSIM = ((2*rxMean*ryMean+C1)*C2)/((rxMean*rxMean+ryMean*ryMean+C1)*(rxVar+C2));
        double gSSIM = ((2*gxMean*gyMean+C1)*C2)/((gxMean*gxMean+gyMean*gyMean+C1)*(gxVar+C2));
        double bSSIM = ((2*bxMean*byMean+C1)*C2)/((bxMean*bxMean+byMean*byMean+C1)*(bxVar+C2));

        return (0.2989*rSSIM+0.5870*gSSIM+0.1140*bSSIM);
    }

    public static int getSerializedSize(QuadTreeNode root) throws IOException{
        ByteArrayOutputStream size = new ByteArrayOutputStream(); // size ==> to write the quadtree
        ObjectOutputStream oos = new ObjectOutputStream(size); //oos ==> into bytes
        oos.writeObject(root);
        oos.flush();
        oos.close();
        return size.toByteArray().length;
    }
}
