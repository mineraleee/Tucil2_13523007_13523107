import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;

class QuadTreeNode {
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
    private static long startTime;
    private static int totalNodes = 0;
    private static int maxDepth = 0;

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
        
        System.out.print("Pilih metode error (1: Variansi, 2: MAD, 3: Max Pixel Difference, 4: Entropy): ");
        method = scanner.nextInt();
        System.out.print("Masukkan threshold: ");
        threshold = scanner.nextDouble();
        System.out.print("Masukkan ukuran blok minimum: ");
        minBlockSize = scanner.nextInt();
        System.out.print("Masukkan nama gambar hasil (beserta ekstensinya .jpg/.png): ");
        String outputName = scanner.next();
        String outputFolder = "../test/result/";
        String outputPath = outputFolder + outputName;

        File resultDir = new File(outputFolder);
        if (!resultDir.exists()) {
            if (resultDir.mkdirs()) {
                System.out.println("Folder 'result' berhasil dibuat.");
            } else {
                System.out.println("Gagal membuat folder 'result'. Pastikan program memiliki izin menulis.");
                return;
            }
        }

        int originalSize = img.getWidth() * img.getHeight() * 3;

        startTime = System.nanoTime();
        QuadTreeNode root = buildQuadTree(img, 0, 0, img.getWidth(), img.getHeight(), 0);
        long executionTime = System.nanoTime() - startTime;

        BufferedImage compressedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        drawQuadTree(compressedImage, root);
        ImageIO.write(compressedImage, "png", new File(outputPath));

        int compressedSize = totalNodes * 4; // Asumsi tiap simpul menyimpan 4 byte data
        double compressionPercentage = 100.0 - ((double) compressedSize / originalSize * 100);

        System.out.println("Waktu eksekusi: " + (executionTime / 1e6) + " ms");
        System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
        System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
        System.out.println("Persentase kompresi: " + String.format("%.2f", compressionPercentage) + "%");
        System.out.println("Kedalaman maksimal pohon: " + maxDepth);
        System.out.println("Total simpul pohon: " + totalNodes);
        System.out.println("Gambar hasil disimpan di: " + outputPath);
    }

    private static QuadTreeNode buildQuadTree(BufferedImage img, int x, int y, int width, int height, int depth) {
        totalNodes++;
        maxDepth = Math.max(maxDepth, depth);
        
        if (width <= minBlockSize || height <= minBlockSize || computeError(img, x, y, width, height) <= threshold) {
            return new QuadTreeNode(x, y, width, height, averageColor(img, x, y, width, height), true);
        }
    
        int halfWidth = width / 2;
        int halfHeight = height / 2;
    
        QuadTreeNode[] children = new QuadTreeNode[4];
        children[0] = buildQuadTree(img, x, y, halfWidth, halfHeight, depth + 1); // Kiri Atas
        children[1] = buildQuadTree(img, x + halfWidth, y, width - halfWidth, halfHeight, depth + 1); // Kanan Atas
        children[2] = buildQuadTree(img, x, y + halfHeight, halfWidth, height - halfHeight, depth + 1); // Kiri Bawah
        children[3] = buildQuadTree(img, x + halfWidth, y + halfHeight, width - halfWidth, height - halfHeight, depth + 1); // Kanan Bawah
    
        QuadTreeNode node = new QuadTreeNode(x, y, width, height, averageColor(img, x, y, width, height), false);
        node.children = children;
        return node;
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

    private static double computeError(BufferedImage img, int x, int y, int width, int height) {
        switch (method) {
            case 1: return computeVariance(img, x, y, width, height);
            case 2: return computeMAD(img, x, y, width, height);
            case 3: return computeMaxDiff(img, x, y, width, height);
            // case 4: return computeEntropy(img, x, y, width, height);
            default: return computeVariance(img, x, y, width, height);
        }
    }

    private static double computeVariance(BufferedImage img, int x, int y, int width, int height) {
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

        if (count == 0) return 0; // Hindari pembagian dengan nol

        double rMean = (double) rSum / count;
        double gMean = (double) gSum / count;
        double bMean = (double) bSum / count;

        // Hitung varians tiap kanal warna
        double rVar = 0, gVar = 0, bVar = 0;

        for (int i = x; i < x + width && i < img.getWidth(); i++) {
            for (int j = y; j < y + height && j < img.getHeight(); j++) {
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
        return (rVar + gVar + bVar) / 3;
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
                madr += r * r;
                madg += g * g;
                madb += b * b;
            }
        }

        madr /= count;
        madg /= count;
        madb /= count;

        // Hitung varians RGB gabungan
        return (madr + madg + madb) / 3;    
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

    // private static double computeEntropy(BufferedImage img, int x, int y, int size) {
        
    // }

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
}
