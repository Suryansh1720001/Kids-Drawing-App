    package com.google.suryansh7202.kidsdrawingapp

    import android.Manifest
    import android.app.Dialog
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.graphics.Canvas
    import android.graphics.Color
    import android.media.MediaScannerConnection
    import android.os.Build
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.provider.MediaStore
    import android.view.View
    import android.widget.*
    import androidx.activity.result.ActivityResultLauncher
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.view.get
    import androidx.lifecycle.lifecycleScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.io.ByteArrayOutputStream
    import java.io.File
    import java.io.FileOutputStream

    class MainActivity : AppCompatActivity() {
        private var drawingView: DrawingView? = null
        private var mImageButtonCurrentPaint: ImageButton? = null
        private var ib_gallery: ImageButton? = null
        private var ib_undo: ImageButton? = null
        private var ib_save: ImageButton? = null
        var customPrivateDialog : Dialog?=null

        val openGalleryLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val imageBackground: ImageView = findViewById(R.id.iv_background)
                    imageBackground.setImageURI(result.data?.data)
                }
            }

        private val requestPermission: ActivityResultLauncher<Array<String>> =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permission ->
                permission.entries.forEach {
                    val permissionName = it.key
                    val isGranted = it.value
                    if (isGranted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permission granted for read storage",
                            Toast.LENGTH_LONG
                        ).show()

                        val pickIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)

                    } else {
                        if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                            Toast.makeText(
                                this,
                                "Permission denied for read storage",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    }
                }


            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            drawingView = findViewById(R.id.drawing_view)
            drawingView?.setSizeForBrush(20.toFloat())
            val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

            mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )


            val ib_brush: ImageButton = findViewById(R.id.ib_brush)
            ib_brush.setOnClickListener {
                showBrushSizeChooserDialog()
                Toast.makeText(this, "this", Toast.LENGTH_LONG).show()
            }

            ib_gallery = findViewById(R.id.ib_gallery)
            ib_gallery?.setOnClickListener {
                requestStoragePermission()
            }

            ib_undo = findViewById(R.id.ib_undo)
            ib_undo?.setOnClickListener {
                drawingView?.onClickUndo()
            }

            ib_save  = findViewById(R.id.ib_save)
            ib_save?.setOnClickListener{
                showProgressDialog()
                if(isReadStorgaAllowed()){
                    lifecycleScope.launch{
                        val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                        saveBitmapFile(getBitmapFromView(flDrawingView))
                    }
                }
            }

        }

        private fun getBitmapFromView(view : View) : Bitmap {
            val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(returnedBitmap)
            val bgDrawable = view.background
            if(bgDrawable != null){
                bgDrawable.draw(canvas)
            }else{
                canvas.drawColor(Color.WHITE)
            }

            view.draw(canvas)
            return returnedBitmap
        }


        private suspend fun saveBitmapFile(mBitmap: Bitmap):String{
            var result = ""
            withContext(Dispatchers.IO){
                if(mBitmap != null){
                    try{
                        val bytes = ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                        val f = File(externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidsDrawingApp_" +System.currentTimeMillis()/1000 +".png")
                        val fo = FileOutputStream(f)
                        fo.write(bytes.toByteArray())
                        fo.close()
                        result = f.absolutePath

                        runOnUiThread{
                            cancelProgressDialog()
                            if(result.isNotEmpty()){
                                Toast.makeText(
                                    this@MainActivity,"File saved successfully: $result",Toast.LENGTH_LONG).show()
                                shareImage(result)

                            }else{
                                if(result.isNotEmpty()){
                                    Toast.makeText(
                                        this@MainActivity,"something went wrong saving the file: $result",Toast.LENGTH_LONG).show()
                            }
                        }
                    }


                }
                    catch (e: Exception){
                        result = ""
                        e.printStackTrace()


                    }
                }
            }

            return result

        }

        private fun isReadStorgaAllowed():Boolean{
            val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            return result == PackageManager.PERMISSION_GRANTED

        }

        private fun requestStoragePermission() {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showRationalDialog(
                    "Kids Drawing App",
                    "Kids Drawing App " + "needs to Access your External Storage"
                )
            } else {
                requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE))

            }
        }

        private fun cancelProgressDialog(){
            if(customPrivateDialog !=null){
            customPrivateDialog?.dismiss()
                customPrivateDialog = null
            }
        }

        private fun showProgressDialog(){
            customPrivateDialog = Dialog(this@MainActivity)
            customPrivateDialog?.setContentView(R.layout.dialog_custom_progress)
            customPrivateDialog?.show()
        }


        private fun showRationalDialog(
            title: String,
            message: String,
        ) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create().show()
        }

        private fun showBrushSizeChooserDialog() {
            val brushDialog = Dialog(this)
            brushDialog.setContentView(R.layout.dialog_brush_size)
            brushDialog.setTitle("Brush size: ")
            val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
            smallBtn.setOnClickListener {
                drawingView?.setSizeForBrush(10.toFloat())
                brushDialog.dismiss()
            }
            val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
            mediumBtn.setOnClickListener {
                drawingView?.setSizeForBrush(20.toFloat())
                brushDialog.dismiss()
            }
            val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
            largeBtn.setOnClickListener {
                drawingView?.setSizeForBrush(30.toFloat())
                brushDialog.dismiss()
            }

            brushDialog.show()
        }


        fun paintClicked(view: View) {
            if (view !== mImageButtonCurrentPaint) {
                val imageButton = view as ImageButton
                val colortag = imageButton.tag.toString()
                drawingView?.setColor(colortag)

                imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
                )
                mImageButtonCurrentPaint?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )


                mImageButtonCurrentPaint = view


            }
        }

        private fun shareImage(result:String){
            MediaScannerConnection.scanFile(this, arrayOf(result),null){
                path,uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent,"Share"))

            }
        }
    }