

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Converters {


    interface OnBitmapConvertListener {
        fun onBitmapConverted(file: File)
    }
     // This subscription needs to be disposed off to release the system resources primarily held for purpose.

    @JvmStatic
    fun convertBitmapToFile(bitmap: Bitmap, onBitmapConvertListener: OnBitmapConvertListener): Disposable {
        return Observable.just(bitmap).subscribeOn(Schedulers.io())
                .subscribe({
                    val file = compressBitmap(it)!!
                    Log.i("convertedPicturePath", file.path)
                    onBitmapConvertListener.onBitmapConverted(file)
                }, { it.printStackTrace() })
    }

    private fun compressBitmap(bitmap: Bitmap): File? {
        //create a file to write bitmap data

        try {
            val myStuff = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "My Stuff")
            if (!myStuff.exists())
                myStuff.mkdirs()
            val picture = File(myStuff, "MyStuff-" + System.currentTimeMillis() + ".png")

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
            val bitmapData = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(picture)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            return picture
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }


}