package com.sts.sontalksign.feature.conversation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.naver.speech.clientapi.SpeechRecognitionResult
import com.sts.sontalksign.R
import com.sts.sontalksign.databinding.ActivityConversationBinding
import com.sts.sontalksign.feature.common.CommonTagItem
import com.sts.sontalksign.feature.common.CustomForm
import com.sts.sontalksign.feature.common.TagSingleton
import com.sts.sontalksign.feature.utils.AudioWriterPCM
import com.sts.sontalksign.global.FileFormats
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ConversationActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityConversationBinding.inflate(layoutInflater)
    }

    private val TAG: String = "ConversationActivity"

    /*CameraX 관련 변수*/
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    private var isNowRecording: Boolean = false //사용자의 "녹음하기" 선택 여부

    //내부저장소 - txt 파일
    private var directory: String? = null
    private var convFilename: String? = null //대화내용 파일명
    private var currentTime: Long? = null

    private lateinit var textList: String

    // naverspeech-sdk-android
    private val CLIENT_ID = "89kna7451i"
    private var handler: RecognitionHandler? = null
    private var naverRecognizer: NaverRecognizer? = null
    private var txtResult: TextView? = null
    private var btnStart: Button? = null
    private var mResult: String? = null
    private var audioWriter: AudioWriterPCM? = null

    private fun handleMessage(msg: Message) {
        when (msg.what) {
            R.id.clientReady -> {
                txtResult!!.text = "Connected"
                audioWriter = AudioWriterPCM(
                    filesDir.absolutePath + "/NaverSpeechTest")
                audioWriter!!.open("Test")
            }
            R.id.audioRecording -> audioWriter?.write(msg.obj as ShortArray)
            R.id.partialResult -> {
                mResult = msg.obj as String
                txtResult!!.text = mResult
            }
            R.id.finalResult -> {
                val speechRecognitionResult = msg.obj as SpeechRecognitionResult
                val results = speechRecognitionResult.results
                val strBuf = StringBuilder()
                for (result in results) {
                    strBuf.append(result)
                    strBuf.append("\n")
                }
                mResult = strBuf.toString()
                txtResult!!.text = mResult
            }
            R.id.recognitionError -> {
                audioWriter?.close()
                mResult = "Error code : ${msg.obj}"
                txtResult!!.text = mResult
                btnStart!!.setText(R.string.str_start)
                btnStart!!.isEnabled = true
            }
            R.id.clientInactive -> {
                audioWriter?.close()
                btnStart!!.setText(R.string.str_start)
                btnStart!!.isEnabled = true
            }
        }
    }

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

        /*이벤트 리스너 설정*/
        //텍스트 입력용 EditText 클릭
        binding.etTextConversation.setOnEditorActionListener { textView, actionId, keyEvent ->
            var handled = false
            //완료버튼 클릭에만 처리
            if(actionId == EditorInfo.IME_ACTION_DONE) {
                addTextLine(textView.text.toString(), false)
                binding.etTextConversation.setText("")
            }
            handled
        }
        //"대화 종료" 버튼 클릭
        binding.btnStopConversation.setOnClickListener { stopConversation() }

        //대화 시작 - "녹음하기" 여부 저장
        isNowRecording = intent.getBooleanExtra("isRecord", false)
        
        /*대화내용 저장*/
        //내부저장소의 경로 저장
        directory = filesDir.absolutePath //내부경로의 절대 경로

        loadTagList()

        txtResult = findViewById<View>(R.id.txt_result) as TextView
        btnStart = findViewById<View>(R.id.btn_start) as Button

        handler = RecognitionHandler(this)
        naverRecognizer = NaverRecognizer(this, handler!!, CLIENT_ID)

        btnStart?.setOnClickListener {
            if (!naverRecognizer?.getSpeechRecognizer()?.isRunning!!) {
                mResult = ""
                txtResult?.text = "Connecting..."
                btnStart!!.setText(R.string.str_stop)
                naverRecognizer?.recognize()
            } else {
                Log.d(TAG, "stop and wait Final Result")
                btnStart?.isEnabled = false
                naverRecognizer?.getSpeechRecognizer()?.stop()
            }
        }
    }

    fun loadTagList() {
        //tagList는 최초 1회만 로드
        if(TagSingleton.tagList.size > 0) return

        Log.d(TAG, "Directory : " + directory)
        val file = File(directory)


        //파일 미존재
        if(!file.exists()) {
            Log.d(TAG, "TAGS file does not exist!!")
            file.mkdirs()
            //return
        }

        val tagFN = "TAGS.txt"
        val fPath = directory + "/" + tagFN
        val writer = FileWriter(fPath, true)

        val reader = FileReader(fPath)
        val buffer = BufferedReader(reader)

        var line: String? = ""
//        var result = StringBuffer()

        while(true) {
            line = buffer.readLine() //줄 단위로 read
            if(line == null) break
            else {
                val (index, text) = line.split(" ")
                TagSingleton.tagList.add(CommonTagItem(index, text))
            }
        }

        val colorList = resources.getIntArray(R.array.tagColorArr)
        for(color in colorList) {
            TagSingleton.colorList.add((color))
        }
        TagSingleton.tagList.add(CommonTagItem("0", "TEST"))

        buffer.close()
    }

    //대화 내용 기록
    private fun createTextFile() {
        //녹음하기 미선택의 경우
        if(!isNowRecording) return

        //내부 저장소 경로
        currentTime = System.currentTimeMillis() //ms로 반환
        directory = filesDir.absolutePath //내부경로의 절대 경로
        convFilename = FileFormats.dataFormat.format(currentTime) + ".txt"

        textList = ""
    }

    private fun getMyConversation(content:String, time:String) : String {
        return getString(R.string.my_conversation_content) + content + getString(R.string.my_conversation_time) + time
    }

    private fun getYourConversation(content:String, time:String) : String {
        return getString(R.string.your_conversation_content) + content + getString(R.string.your_conversation_time) + time
    }

    //isMine - 0:나의 대사, 1:상대의 대사
    private fun addTextLine(content:String, isMine:Boolean) {
        //녹음하기 미선택의 경우
        if(!isNowRecording) return

        var bContent = String()
        currentTime = System.currentTimeMillis()
        when(isMine) {
            true -> bContent = getMyConversation(content, FileFormats.timeFormat.format(currentTime))
            false -> bContent = getYourConversation(content, FileFormats.timeFormat.format(currentTime))
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
        val writer = FileWriter(directory + "/" + convFilename, true)

        //쓰기 속도 향상
        val buffer = BufferedWriter(writer)
        buffer.write(result)
        buffer.close()
    }

    //TODO: 삭제 예정
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

    //대화 종료 처리 함수
    private fun stopConversation() {
        Log.d(TAG, "stopConversation() START")

        //녹음하기 선택 시 - 팝업 발생 및 대화 내용 저장
        if(isNowRecording) {
            val cForm = CustomForm(this)
            cForm.show()
            cForm.setOnBtnStoreClickedListener(object: CustomForm.onBtnStoreClickedListener {
                override fun onBtnStoreClicked(title: String, tags: String) {
                    val rConversation = title + "\nTAGS_" + tags + "\n" + textList //{제목\n태그인덱스\n대화내용} 형식
                    writeTextFile(rConversation) //대화 종료 전 기록에 쌓인 대화 내용을 저장
                    finish()
                }
            })
        }
        else { //녹음하기 미선택 시 - "대화 종료" 질의 팝업 발생
            //TODO: : "대화를 종료하시겠습니까?" 팝업 생성 및 발생
        }
    }

    /*CameraX 관련 함수*/
    private fun takePhoto() {

    }

    private fun captureVideo() {

    }

    //카메라 시작
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this) //Activity와 카메라의 수명 주기를 binding

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get() //카메라의 수명 주기를 APP 프로세스 내의 LifecycleOwner에 바인딩

            //카메라 프리뷰 초기화 및 설정
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.pvCamera.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA //전면 카메라를 기본으로 선택

            try {
                cameraProvider.unbindAll() //바인딩된 항목 전체 제거

                //카메라 관련 객체 바인딩
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc) //앱에 더이상 포커스 없는 경우 등의 실패 케이스 처리
            }

        }, ContextCompat.getMainExecutor(this)) //기본 스레드에서 실행되는 Executor를 반환
    }

    //카메라 권한
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    //카메라 권한 요청 처리
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
        private const val cTAG = "CameraX Preview"
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

    public override fun onStart() {
        super.onStart()
        naverRecognizer?.getSpeechRecognizer()?.initialize()
    }

    public override fun onResume() {
        super.onResume()
        mResult = ""
        txtResult?.text = ""
        btnStart?.setText(R.string.str_start)
        btnStart?.isEnabled = true
    }

    public override fun onStop() {
        super.onStop()
        naverRecognizer?.getSpeechRecognizer()?.release()
    }

    internal class RecognitionHandler(activity: ConversationActivity) : Handler() {
        private val mActivity: WeakReference<ConversationActivity>

        init {mActivity = WeakReference(activity)}
        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            activity?.handleMessage(msg)
        }
    }
}