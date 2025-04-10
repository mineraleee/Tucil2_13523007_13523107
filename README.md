# Kompresi Gambar Dengan Metode Quadtree 

Program ini merupakan implementasi algoritma kompresi gambar menggunakan struktur data QuadTree dengan pendekatan Divide and Conquer. Gambar diubah menjadi matriks piksel berbasis sistem warna RGB, lalu dibagi secara rekursif menjadi blok-blok kecil berdasarkan tingkat keseragaman warna. Proses pembagian blok dilakukan dengan membandingkan nilai error (seperti variansi, MAD, max pixel difference, atau entropy) terhadap nilai ambang batas (threshold) yang ditentukan dan ukuran minimum block. Jika nilai error melebihi threshold dan ukuran blok masih di atas batas minimum, maka blok akan terus dibagi menjadi empat sub-blok. 

Setiap blok akhir yang tidak lagi dibagi akan dinormalisasi warnanya menggunakan rata-rata nilai RGB di dalamnya. Hasil dari kompresi disimpan dalam bentuk gambar baru, disertai informasi ukuran awal dan akhir serta persentase efisiensi kompresi yang dicapai. Struktur QuadTree memungkinkan pengurangan ukuran file secara signifikan tanpa menghilangkan detail penting pada gambar.

## Instalasi

Program ini ditulis dalam bahasa Java dan semua pustaka yang diperlukan sudah tersedia dalam pustaka standar Java.
Pastikan Java telah terpasang di sistem Anda. Anda dapat memverifikasinya dengan menjalankan perintah berikut:
```python
java -version
```
## Struktur Program
```
Tucil2_13523007_13523107/
├── bin/
├── doc/
│   ├── Tucil2_13523007_13523107.pdf
├── src/
│   ├── GifSequenceWriter.class
│   ├── GifSequenceWriter.java
│   ├── QuadTreeCompression.class   
│   ├── QuadTreeCompression.java   
│   ├── QuadTreeNode.class 
├── test/
│   ├── result/
│   │     ├── result1.jpg
│   │     ├── result1.gif
│   │     ├── result2.jpg
│   │     ├── result2.gif
│   │     ├── result3.jpg
│   │     ├── result3.gif
│   │     ├── result4.jpg
│   │     ├── result4.gif
│   │     ├── result5.jpg
│   │     ├── result5.gif
│   │     ├── result6.jpg
│   │     ├── result6.gif
│   │     ├── result7.jpg
│   │     ├── result7.gif
│   ├── source/
│   │     ├── try1.jpg
│   │     ├── try2.jpg
│   │     ├── try3.jpg
│   │     ├── try4.jpg
│   │     ├── try5.jpg
│   │     ├── try6.jpg
│   │     ├── try7.jpg
└── README.md         
```
## Cara Menjalankan
1. **Clone repository**
```python
git clone https://github.com/mineraleee/Tucil2_13523007_13523107
```
2. **Masuk ke directory src**
```python
cd src
```
3. **Kompilasi file Java**
```python
javac QuadTreeCompression.java
```
4. **Jalankan program melalui CLI**

```python
java QuadTreeCompression
```
Setelah menjalankan program melalui CLI, akan muncul serangkaian permintaan input dari pengguna, yaitu:

a. **[INPUT]** Alamat absolut dari gambar yang akan dikompresi.

b. **[INPUT]** Metode perhitungan error (gunakan angka sesuai pilihan yang tersedia).

c. **[INPUT]** Nilai ambang batas (threshold), pastikan nilainya sesuai dengan metode yang dipilih.

d. **[INPUT]** Target persentase kompresi (dalam bentuk angka desimal). Masukkan 0 jika ingin menonaktifkan mode ini. Jika diaktifkan, maka threshold akan disesuaikan secara otomatis agar memenuhi target kompresi.

e. **[INPUT]** Ukuran blok minimum yang diizinkan.

f. **[INPUT]** Alamat absolut tempat menyimpan gambar hasil kompresi.

g. **[INPUT]** Alamat absolut file GIF.

## Kontributor

| NIM      | Nama  | Kelas |
| :---:    | :---: | :---: |
| 13523007| Ranashahira Reztaputri|01|
| 13523107| Heleni Gratia Meitrina Tampubolon|02|