package com.sts.sontalksign.feature.conversation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sts.sontalksign.R
import com.sts.sontalksign.databinding.ActivityConversationBinding
import com.sts.sontalksign.feature.common.CustomForm
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConversationActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityConversationBinding.inflate(layoutInflater)
    }

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private var isNowRecording: Boolean = false

    //내부저장소 - txt 파일
    private var directory: String? = null
    private var filename: String? = null
    private var currentTime: Long? = null
    private val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val timeFormat = SimpleDateFormat("HH:mm")

    private lateinit var textList: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //카메라 권한 요청
        if(allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        //이벤트 리스너 설정
        binding.etTextConversation.setOnEditorActionListener { textView, actionId, keyEvent ->
            var handled = false
            if(actionId == EditorInfo.IME_ACTION_DONE) {
                addTextLine(textView.text.toString(), false)
                binding.etTextConversation.setText("")
            }
            handled
        }
        binding.btnStopConversation.setOnClickListener { stopConversation() }

        isNowRecording = intent.getBooleanExtra("isRecord", false)
        createTextFile()
    }

    //대화 내용 기록
    private fun createTextFile() {
        //녹음하기 미선택의 경우
        if(!isNowRecording) return

        //내부 저장소 경로
        currentTime = System.currentTimeMillis() //ms로 반환
        directory = filesDir.absolutePath //내부경로의 절대 경로
        filename = dataFormat.format(currentTime) + ".txt"

        textList = ""
    }

    private fun getMyConversation(content:String, time:String) : String {
        return getString(R.string.my_conversation_tag) + content + getString(R.string.my_conversation_tag) + time
    }

    private fun getYourConversation(content:String, time:String) : String {
        return getString(R.string.your_conversation_tag) + content + getString(R.string.your_conversation_tag) + time
    }

    //isMine - 0:나의 대사, 1:상대의 대사
    private fun addTextLine(content:String, isMine:Boolean) {
        //녹음하기 미선택의 경우
        if(!isNowRecording) return

        var bContent = String()
        currentTime = System.currentTimeMillis()
        when(isMine) {
            true -> bContent = getMyConversation(content, timeFormat.format(currentTime))
            false -> bContent = getYourConversation(content, timeFormat.format(currentTime))
        }

        textList += bContent
    }

    //파일 쓰기
    private fun writeTextFile(result: String) {
        val dir = File(directory)

        //파일 미존재 시 디렉토리 및 파일 생성
        if(!dir.exists()) {
            dir.mkdirs()
        }

        //파일의 full path
        val writer = FileWriter(directory + "/" + filename, true)

        //쓰기 속도 향상
        val buffer = BufferedWriter(writer)
        buffer.write(result)
        buffer.close()
    }

    //파일 읽기 - ConversationActivity에서는 미사용
    private fun readTextFile(fullpath: String) : String {
        val file = File(fullpath)

        //파일 미존재
        if(!file.exists()) return ""

        val reader = FileReader(file)
        val buffer = BufferedReader(reader)

        var line:String? = ""
        var result = StringBuffer()

        while(true) {
            line = buffer.readLine() //줄단위로 read
            if(line == null) break
            else result.append(line).append("\n")
        }

        buffer.close()
        return result.toString()
    }

    private fun takePhoto() {

    }

    private fun captureVideo() {

    }

    private fun startCamera() {
        //Activity와 카메라의 수명 주기를 binding
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            //카메라의 수명 주기를 APP 프로세스 내의 LifecycleOwner에 바인딩
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //카메라 프리뷰 초기화 및 설정
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.pvCamera.surfaceProvider)
                }

            //전면 카메라를 기본으로 선택
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                //바인딩된 항목 전체 제거
                cameraProvider.unbindAll()

                //카메라 관련 객체 바인딩
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch(exc: Exception) {
                //앱에 더이상 포커스 없는 경우 등의 실패 케이스 처리
                Log.e(TAG, "Use case binding failed", exc)
            }

            //기본 스레드에서 실행되는 Executor를 반환
        }, ContextCompat.getMainExecutor(this))
    }

    //대화 종료 처리 함수
    private fun stopConversation() {
        lateinit var cTitle: String
        lateinit var cTags: String

        //팝업을 띄운다
        val cForm = CustomForm(this)
        cForm.show()
        cForm.setOnBtnStoreClickedListener(object: CustomForm.onBtnStoreClickedListener {
            override fun onBtnStoreClicked(title: String, tags: String) {
                cTitle = title
                cTags = tags

                val rConversation = cTitle + "\nTAGS_" + cTags + "\n" + textList
                //대화 종료 전 기록에 쌓인 대화 내용을 저장
                writeTextFile(rConversation)

                finish()
            }
        })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {

        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraX Preview"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}