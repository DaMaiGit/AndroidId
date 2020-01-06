package com.example.androidid

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.util.*




class MainActivity : AppCompatActivity() {
    private lateinit var tvIMEI1: TextView
    private lateinit var tvIMEI2: TextView
    private lateinit var tvMEID: TextView
    private lateinit var tvMac:TextView
    private lateinit var tvIMSI1:TextView
    private lateinit var tvIMSI2:TextView
    private lateinit var tvICCID1:TextView
    private lateinit var tvICCID2:TextView
    private lateinit var tvSN:TextView
    private lateinit var tvAndroidId:TextView
    private lateinit var tvSimCount:TextView
    private lateinit var tvSimMaxCount:TextView
    private lateinit var tvSimNetType:TextView
    private lateinit var btn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        getPhonePermission()
        btn.setOnClickListener(View.OnClickListener {
            //获取IMEI或MEID
            getIMEIORMEID()
            //获取MEID
            getMEID()
            //获取mac
            tvMac.text = "MAC:${getMacAddress(this)}"
            //获取IMSI
            getIMSI()
            //TelephonyManager获取ICCID
            getICCIDByTM()
            //SubscriptionManager获取ICCID
            getICCIDBySM()
            //获取SN
            getSN()
            //Android ID
            getAndroidId()
            tvSimCount.text = "当前Sim卡数量：${checkSimCount(this)}"
            getMaxSimCount(this)
            getNetType()
        })
    }


    /**
     * 初始化控件
     */
    private fun init(){
        tvIMEI1 = findViewById(R.id.tv_imei_one)
        tvIMEI2 = findViewById(R.id.tv_imei_two)
        tvMEID = findViewById(R.id.tv_meid)
        tvMac = findViewById(R.id.tv_mac)
        tvIMSI1 = findViewById(R.id.tv_imsi_one)
        tvIMSI2 = findViewById(R.id.tv_imsi_two)
        tvICCID1 = findViewById(R.id.tv_iccid_one)
        tvICCID2 = findViewById(R.id.tv_iccid_two)
        tvSN = findViewById(R.id.tv_sn)
        tvAndroidId = findViewById(R.id.tv_android_id)
        tvSimCount = findViewById(R.id.tv_simCount)
        tvSimMaxCount = findViewById(R.id.tv_simMaxCount)
        tvSimNetType = findViewById(R.id.tv_simNetType)
        btn = findViewById(R.id.btn)
    }

    /**
     * 获取安卓权限
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun getPhonePermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE),1)
        }

    }

    /**
     * 获取IMEI或MEID
     */
    fun getIMEIORMEID(){
        //检查是否有READ_PHONE_STATE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED){
            //获取IMEI和MEID
            val tm = getSystemService<TelephonyManager>()
            //getDeviceId()从API1就已经存在，默认返回卡槽一的IMEI或MEID
            var imei: String? = tm?.getDeviceId()
            //getDeviceId(solotIndex)在API23加入可以通过指定卡槽位置获取IMEI或MEID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //solotIndex:0 -> 卡槽1 IMEI或MEID
                tvIMEI1.text = "卡槽一 IMEI或MEID：${tm?.getDeviceId(0)}"
                //solotIndex:1 -> 卡槽2 IMEI或MEID
                tvIMEI2.text = "卡槽二 IMEI或MEID：${tm?.getDeviceId(1)}"
            }
        }
    }

    /**
     * 获取MEID
     */
    fun getMEID(){
        //检查是否有READ_PHONE_STATE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED){
            //获取IMEI和MEID
            val tm = getSystemService<TelephonyManager>()
            //获取MEID在API26加入
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //返回默认可用卡槽的MEID
                var meid:String? = tm?.meid
                //可以通过指定卡槽获取MEID
                tvMEID.text = "MEID：${tm?.getMeid(0)}"
            }
        }
    }

    /**
     * ANDROID6.0以前
     */
    fun getMacDefault(context:Context):String{
        var mac = "02:00:00:00:00:00"
        var wifi:WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var wifiInfo:WifiInfo =wifi.connectionInfo
        mac = wifiInfo.macAddress
        return mac
    }

    /**
     * ANDROID6.0-7.0
     */
    private fun getMacFromFile(): String {
        var WifiAddress = "02:00:00:00:00:00"
        try {
            WifiAddress =
                BufferedReader(FileReader(File("/sys/class/net/wlan0/address"))).readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return WifiAddress
    }

    /**
     * Android7.0之后
     */
    private fun getMacFromHardware(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!(nif.name.equals("wlan0",ignoreCase = true))) continue

                val macBytes = nif.hardwareAddress ?: return ""

                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }

                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "02:00:00:00:00:00"
    }

    /**
     * 获取MAC地址
     */
    fun getMacAddress(context: Context): String {
        var mac = "02:00:00:00:00:00"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mac = getMacDefault(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mac = getMacFromFile()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mac = getMacFromHardware()
        }
        return mac
    }

    fun getIMSI(){
        //检查是否有READ_PHONE_STATE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED){
            //获取IMSI
            val tm = getSystemService<TelephonyManager>()
            //在SDKAPI 1就存在getSubscriberId（）方法，返回默认卡槽IMSI
            var imsi = tm?.subscriberId
            //在SDKAPI 21加入getSubscriberId(subId)来指定返回卡槽位置IMSI
            //在SDKAPI 29以上，指定卡槽位置的方法不再暴露，但依旧能通过反射来获取
            tvIMSI1.text = "卡槽一 IMSI：${getReflectMethod(this,"getSubscriberId",0) as CharSequence?}"
            tvIMSI2.text = "卡槽二 IMSI：${getReflectMethod(this,"getSubscriberId",1) as CharSequence?}"

        }
    }

    /**
     * 通过反射调用
     */
    fun getReflectMethod(context: Context, method: String, param: Int): Any? {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            val telephonyClass = Class.forName(telephony.javaClass.name)
            val parameter = arrayOfNulls<Class<*>>(1)
            parameter[0] = Int::class.javaPrimitiveType
            val getSimState = telephonyClass.getMethod(method, *parameter)
            val ob_phone = getSimState.invoke(telephony, param)

            if (ob_phone != null) {
                return ob_phone
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 通过TelephonyManager获取CCID
     */
    fun getICCIDByTM(){
        //检查是否有READ_PHONE_STATE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED){
            //获取IMSI
            val tm = getSystemService<TelephonyManager>()
            //在SDKAPI 1就存在getSubscriberId（）方法，返回默认卡槽IMSI
            var iccid = tm?.simSerialNumber
            //在SDKAPI 21加入getSubscriberId(subId)来指定返回卡槽位置IMSI
            //在SDKAPI 29以上，指定卡槽位置的方法不再暴露，但依旧能通过反射来获取
            tvICCID1.text = "卡槽一 ICCID：${getReflectMethod(this,"getSimSerialNumber",0) as CharSequence?}"
            tvICCID2.text = "卡槽二 ICCID：${getReflectMethod(this,"getSimSerialNumber",1) as CharSequence?}"

        }
    }

    /**
     * 通过SubscriptionManager获取CCID SDK API 22及以上使用
     */
    fun getICCIDBySM(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val mSubscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            //检查是否有READ_PHONE_STATE权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED){
                //获取当前有效的SIM卡列表
                var subInfos = mSubscriptionManager.activeSubscriptionInfoList
                for ((index,item) in subInfos.withIndex()){
                    when(index){
                        0->tvICCID1.text = "卡槽一 ICCID：${item.iccId}"
                        1->tvICCID2.text = "卡槽二 ICCID：${item.iccId}"
                    }
                }
            }
        }
    }

    /**
     * 获取SN
     */
    fun getSN(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED){
                // 需要READ_PHONE_STATE权限
                tvSN.text = "SN：${Build.getSerial()}"
            }
        }else{
            tvSN.text = "SN：${Build.SERIAL}"
        }
    }

    /**
     * 获取AndroidId
     */
    fun getAndroidId(){
        tvAndroidId.text = "Android ID：${Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID)}"
    }


    /**
     * 获取Sim卡个数
     * @param context
     * @return
     */
    fun checkSimCount(context: Context): Int {
        var simCount: Int
        try {
            //如果SDK Version >=22 && 拥有Manifest.permission.READ_PHONE_STATE 权限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                simCount = getSimCountBySubscriptionManager(context)
                if (simCount == -1) {
                    //如果SubscriptionManager获取失败，则TelephonyManager尝试获取
                    simCount = getSimCountByTelephonyManager(context)
                }
            } else {
                simCount = getSimCountByTelephonyManager(context)
            }

        } catch (e: Exception) {
            simCount = getSimCountByTelephonyManager(context)
        }

        return simCount
    }


    /**
     * 通过SubscriptionManager获取Sim卡张数，如果获取失败返回-1
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun getSimCountBySubscriptionManager(context: Context): Int {
        try {
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            return subscriptionManager?.activeSubscriptionInfoCount ?: -1
        } catch (e: Throwable) {
            return -1
        }

    }

    /**
     * 通过TelephonyManager反射获取Sim卡张数
     * @param context
     * @return
     */
    private fun getSimCountByTelephonyManager(context: Context): Int {
        val slotOne = getSimStateBySlotIdx(context, 0)
        val slotTwo = getSimStateBySlotIdx(context, 1)
        if (slotOne && slotTwo) {
            return 2
        } else if (slotOne || slotTwo) {
            return 1
        } else {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simState = telephony.simState
            return if (simState != TelephonyManager.SIM_STATE_ABSENT && simState != TelephonyManager.SIM_STATE_UNKNOWN) {
                1
            } else 0
        }
    }


    /**
     * 通过反射去调用TelephonyManager.getSimState(int slotIdx)方法，获取sim卡状态
     * SIM_STATE_UNKNOWN 0 SIM卡状态：未知
     * SIM_STATE_ABSENT 1 SIM卡状态：设备中没有SIM卡
     * SIM_STATE_PIN_REQUIRED 2 SIM卡状态：锁定：要求用户的SIM卡PIN解锁
     * SIM_STATE_PUK_REQUIRED 3 SIM卡状态：锁定：要求用户的SIM PUK解锁
     * SIM_STATE_NETWORK_LOCKED 4 SIM卡状态：锁定：需要网络PIN才能解锁
     * SIM_STATE_READY 5 SIM卡状态：就绪
     * SIM_STATE_NOT_READY 6 SIM卡状态：SIM卡尚未就绪
     * SIM_STATE_PERM_DISABLED 7 SIM卡状态：SIM卡错误，已永久禁用
     * SIM_STATE_CARD_IO_ERROR 8 SIM卡状态：SIM卡错误，存在但有故障
     * SIM_STATE_CARD_RESTRICTED 9 SIM卡状态：SIM卡受限，存在，但由于运营商的限制而无法使用。
     * @param context
     * @param slotIdx:0(sim1),1(sim2)
     * @return
     */
    fun getSimStateBySlotIdx(context: Context, slotIdx: Int): Boolean {
        var isReady = false
        try {
            val getSimState = getSimByMethod(context, "getSimState", slotIdx)
            if (getSimState != null) {
                val simState = Integer.parseInt(getSimState.toString())
                if (simState != TelephonyManager.SIM_STATE_ABSENT && simState != TelephonyManager.SIM_STATE_UNKNOWN) {
                    isReady = true
                }
            }
        } catch (e: Throwable) {
        }

        return isReady
    }


    /**
     * 通过反射获取Sim卡状态
     * @param context
     * @param method
     * @param param
     * @return
     */
    private fun getSimByMethod(context: Context, method: String, param: Int): Any? {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            val telephonyClass = Class.forName(telephony.javaClass.name)
            val parameter = arrayOfNulls<Class<*>>(1)
            parameter[0] = Int::class.javaPrimitiveType
            val getSimState = telephonyClass.getMethod(method, *parameter)
            val obParameter = arrayOfNulls<Any>(1)
            obParameter[0] = param
            val result = getSimState.invoke(telephony, *obParameter)

            if (result != null) {
                return result
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 获取当前支持最多sim卡数量
     */
    fun getMaxSimCount(context: Context){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                tvSimMaxCount.text =  "当前支持最多Sim卡数量：${subscriptionManager?.activeSubscriptionInfoCountMax}"
            }

        } catch (e: Throwable) {
        }
    }

    /**
     * 通过TelephonyManager当前蜂窝网络运营商信息
     */
    fun getNetType(){
        //检查是否有READ_PHONE_STATE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED){
            //获取TelephonyManager
            val tm = getSystemService<TelephonyManager>()
            //在SDK API 1 getSimOperator（）方法
            when(tm?.simOperator){
                "46000","46002","46004","46007","46008"->tvSimNetType.text = "当前上网SIM卡运营商：移动"
                "46001","46006","46009"->tvSimNetType.text = "当前上网SIM卡运营商：联通"
                "46003","46005","46011"->tvSimNetType.text = "当前上网SIM卡运营商：电信"
                "46020"->tvSimNetType.text = "当前上网SIM卡运营商：铁通"
            }

        }
    }



}
