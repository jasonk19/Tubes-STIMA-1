#Strategi Algoritama - Bot Greedy

##Bot pada Game Overdrive dengan menggunakan Prinsip Greedy

###Requirements
1. [Java](https://www.oracle.com/java/technologies/downloads/#java8)
2. [IntelliJ IDEA](https://www.jetbrains.com/idea/)
3. [NodeJS](https://nodejs.org/en/download/)

###Cara Main
1. Clone Repo ini dan download Entelect Challenge2020 starter pack [disini](https://github.com/EntelectChallenge/2020-Overdrive/releases/tag/2020.3.4)
2. Buka folder hasil clone dengan menggunakan IntelliJ IDEA lalu Copy file yang ada di staterpack tersebut kecuali folder starter-bots
3. Lalu dengan Tools run pada IntelliJ IDEA pilih game-runner-jar-with-depedencies.jar untuk di run.
##Algoritma Greedy
###Greedy by Obstacle
Greedy by Obstacle adalah strategi yang berfokus pada menghindari suatu Terrain yang memberikan damage ke mobil. Pada setiap ronde, periksalah apakah di depan mobil terdapat Terrain yang bersifat obstacle atau tidak. Tingkatan menghindari suatu Terrain berdasarkan besarnya slowing dari terrain, semakin besar slowing maka itu menjadi prioritas utama untuk dihindari.  Jika ada obstacle, maka jalankan command untuk menghindari Terrain, jika tidak, maka jalankan command yang lain. Akan tetapi, jika memiliki powerup Lizard, maka saat tidak ditemukan lane kosong tetapi memiliki powerup Lizard, maka command lizard dijalankan untuk melompati obstacle
###Greedy by Power-up
Greedy by PowerUp adalah strategi greedy yang mengutamakan pada penggunaan power-up jika terdapat pada inventory (list of powerups) dari bot. Pada setiap ronde, periksa apakah di-inventory tersedia suatu powerup. Jika bot memiliki powerup, maka powerup akan digunakan terlebih dahulu sebelum menjalankan command command yang lain. Untuk meningkatkan efektivitas dari penggunaan powerup, maka powerup digunakan dengan syarat syarat tertentu. Selain menggunakan power-up yang ada di-inventory, Greedy by Powerup juga berusaha untuk mencari powerup yang ada pada lane dalam jangkauan dari bot.
###Greedy by Damage
Greedy by Damage adalah strategi greedy yang mengutamakan dalam meminimumkan besar damage yang diterima oleh bot. Bot akan memilih lane yang memberikan damage terkecil. Algoritma ini hanya akan dijalankan apabila sudah dipastikan bahwa untuk turn selanjutnya bot akan menabrak. Apabila memungkinkan untuk tidak menabrak, maka greedy by obstacle akan dijalankan. Sekilas strategi greedy ini menyerupai strategi Greedy by Obstacle. Hal ini dikarenakan strategi Greedy by Damage merupakan substrategi dari Greedy by Obstacle. 
###Greedy by Speed
Greedy by Speed adalah strategi greedy yang berusaha untuk memaksimalkan speed yang dimiliki dari bot. Strategi ini juga memanfaatkan power-up boost sebagai prioritas powerup karena efeknya yang dapat meningkatkan speed sampai range speed maksimum. Pada strategi greedy ini memfokuskan pada penggunaan command ACCELERATE, BOOST, dan FIX.

#Author
1. 13520032 Fadil Fauzani
2. 13520080 Jason Kanggara
3. 13520089 Nayotama Pradipta