const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.kirimNotifikasiPanggilan = functions.firestore
  .document("antrian/{tanggal}/pasien/{pasienId}")
  .onUpdate(async (change, context) => {
    const dataBaru = change.after.data();
    const dataLama = change.before.data();

    // Cek jika status berubah menjadi "Dipanggil"
    if (dataBaru.status === "Dipanggil" && dataLama.status !== "Dipanggil") {
      const userId = dataBaru.userId;
      const nomorAntrian = dataBaru.nomorAntrian;

      // Ambil token FCM dari koleksi 'users'
      const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();
      if (!userDoc.exists) {
        console.log("User tidak ditemukan:", userId);
        return null;
      }
      
      const fcmToken = userDoc.data().fcmToken;
      if (!fcmToken) {
          console.log("Token FCM tidak ditemukan untuk user:", userId);
          return null;
      }

      // Siapkan payload notifikasi
      const payload = {
        notification: {
          title: "Giliran Anda Telah Tiba!",
          body: `Nomor antrian ${nomorAntrian}, silakan menuju ruang periksa.`,
        },
        token: fcmToken,
      };
      
      console.log("Mengirim notifikasi ke:", fcmToken);

      // Kirim notifikasi
      try {
        const response = await admin.messaging().send(payload);
        console.log("Notifikasi berhasil dikirim:", response);
      } catch (error) {
        console.error("Gagal mengirim notifikasi:", error);
      }
    }
    return null;
  });