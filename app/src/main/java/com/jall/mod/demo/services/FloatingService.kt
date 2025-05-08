package com.jall.mod.demo.services

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.text.InputType
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.jall.mod.demo.R
import com.jall.mod.demo.preferences.AppPreference
import com.jall.mod.demo.preferences.MenuPreference
import java.util.Objects

@Suppress("DEPRECATION")
class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingParams: WindowManager.LayoutParams
    private lateinit var floatingLayout: FrameLayout
    private lateinit var collapseLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var expandParams: LinearLayout.LayoutParams
    private lateinit var menuParams: LinearLayout.LayoutParams
    private lateinit var mainLayout: LinearLayout
    private lateinit var settingLayout: LinearLayout
    private lateinit var ivIcon: ImageView
    private lateinit var name: String

    private var floatingHeight = 0
    private var isSave = false
    private var isAutoSize = false

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("HardwareIds")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val value = super.onStartCommand(intent, flags, startId)
        isStarted = true
        name = intent?.getStringExtra(EXTRA_NAME)!!

        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    NOTIFICATION,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(FOREGROUND_ID, notification)
        init(overlay)
        return value
    }

    override fun onDestroy() {
        super.onDestroy()
        isStarted = false
        windowManager.removeView(floatingLayout)
    }

    private fun init(overlay: Int) {
        floatingParams = WindowManager.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            overlay,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        floatingParams.x = AppPreference.getInt(WINDOW_X, 0)
        floatingParams.y = AppPreference.getInt(WINDOW_Y, 0)
        initFloating()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("SetTextI18n")
    private fun initFloating() {
        val root = RelativeLayout(this)

        val menuLayout = LinearLayout(this).apply {
            layoutParams =
                LinearLayout.LayoutParams(dp(290f), LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(getColor(R.color.md_theme_background))
            orientation = LinearLayout.VERTICAL
        }

        floatingLayout = FrameLayout(this).apply {
            setOnTouchListener(object : View.OnTouchListener {
                var rootX = 0
                var rootY = 0
                var moveX = 0f
                var moveY = 0f

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(p0: View, p1: MotionEvent): Boolean {
                    return when (p1.action) {
                        MotionEvent.ACTION_DOWN -> {
                            rootX = floatingParams.x
                            rootY = floatingParams.y
                            moveX = p1.rawX
                            moveY = p1.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            alpha = 0.8f
                            val movingX = floatingParams.x
                            val movingY = floatingParams.y
                            val display = windowManager.defaultDisplay
                            val metrics = DisplayMetrics()
                            display.getRealMetrics(metrics)
                            val screenX = metrics.widthPixels
                            val screenY = metrics.heightPixels
                            val sideX =
                                if (menuLayout.visibility == View.VISIBLE) dp(290f) else dp(50f)
                            val sideY =
                                if (menuLayout.visibility == View.VISIBLE) floatingHeight else dp(
                                    50f
                                )

                            if (movingX < 0) {
                                floatingParams.x = 0
                                moveX = p1.rawX
                                rootX = 0
                            } else if (movingX > screenX - sideX) {
                                floatingParams.x = screenX - sideX
                                moveX = p1.rawX
                                rootX = floatingParams.x
                            } else {
                                floatingParams.x = rootX + (p1.rawX - moveX).toInt()
                            }

                            if (movingY < 0) {
                                floatingParams.y = 0
                                moveY = p1.rawY
                                rootY = 0
                            } else if (movingY > screenY - sideY) {
                                floatingParams.y = screenY - sideY
                                moveY = p1.rawY
                                rootY = floatingParams.y
                            } else {
                                floatingParams.y = rootY + (p1.rawY - moveY).toInt()
                            }

                            windowManager.updateViewLayout(this@apply, floatingParams)
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            alpha = 1f
                            val touchX = p1.rawX - moveX
                            val touchY = p1.rawY - moveY

                            if (ivIcon.visibility == View.VISIBLE && touchX >= 0 && touchX <= 1 && touchY >= 0 && touchY <= 1) {
                                ivIcon.visibility = View.GONE
                                menuLayout.visibility = View.VISIBLE
                            }

                            AppPreference.setInt(WINDOW_X, floatingParams.x)
                            AppPreference.setInt(WINDOW_Y, floatingParams.y)
                            true
                        }

                        else -> false
                    }
                }
            })
        }

        windowManager.addView(floatingLayout, floatingParams)

        ivIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(50f), dp(50f))
            scaleType = ImageView.ScaleType.FIT_XY
            visibility = View.GONE
            Glide.with(this@FloatingService).load(R.mipmap.ic_launcher)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop().into(this)
        }

        settingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setMenuList(this, arrayOf(
                "-1_Toggle_Save feature preferences",
                "-2_Toggle_Auto size vertically",
                "-3_Button_<font color='#9b333a'>Close mod</font>"
            ))
        }

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setMenuList(this, arrayOf(
                "Toggle_The toggle",
                "100_Toggle_True_The toggle 2",
                "110_Toggle_The toggle 3",
                "SeekBar_The slider_1_100",
                "Spinner_The spinner_Items 1,Items 2,Items 3",
                "Button_The button",
                "ButtonLink_The button with link_https://mod.jall.my.id",
                "InputValue_Input number",
                "InputValue_Input number 2_1000",
                "InputText_Input text",
                "RadioButton_Radio buttons_OFF,Mod 1,Mod 2,Mod 3",
                "Collapse_Collapse 1",
                "CollapseAdd_Toggle_The toggle",
                "CollapseAdd_Toggle_The toggle",
                "123_CollapseAdd_Toggle_The toggle",
                "CollapseAdd_Button_The button",
                "Collapse_Collapse 2",
                "CollapseAdd_SeekBar_The slider_1_100",
                "CollapseAdd_InputValue_Input number",
                "RichTextView_This is text view, not fully HTML" +
                        "<b>Bold</b> <i>italic</i> <u>underline</u>" +
                        "<br/>New line <font color='red'>Support colors</font>" +
                        "<br/><big>bigger Text</big>",
                "CollapseAdd_Button_The button",
            ))
        }

        expandParams = menuLayout.layoutParams as LinearLayout.LayoutParams
        expandParams.weight = 1f
        menuParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210f))

        val header = TextView(this).apply {
            text = "ModJALL - $name"
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(getColor(R.color.md_theme_primary))
            textSize = 18f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(dp(5f), dp(14f), dp(5f), dp(14f))
        }

        scrollView = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.md_theme_background))
            layoutParams = if (isAutoSize) expandParams else menuParams
        }

        menuLayout.viewTreeObserver.addOnGlobalLayoutListener {
            floatingHeight = menuLayout.height
        }

        val navigation = LinearLayout(this).apply {
            setPadding(dp(10f), dp(3f), dp(10f), dp(3f))
        }

        val minimize = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                GradientDrawable().apply {
                    setColor(getColor(R.color.md_theme_background))
                },
                null
            )
            text = "Minimize"
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(getColor(R.color.md_theme_onPrimary))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            isAllCaps = true

            setOnClickListener {
                menuLayout.visibility = View.GONE
                ivIcon.visibility = View.VISIBLE
            }
        }

        val setting = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                GradientDrawable().apply {
                    setColor(getColor(R.color.md_theme_background))
                },
                null
            )
            text = "Setting"
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(getColor(R.color.md_theme_error))
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            isAllCaps = true

            setOnClickListener(object : View.OnClickListener {
                var isClick = false

                override fun onClick(p0: View?) {
                    isClick = !isClick
                    scrollView.scrollTo(0, 0)

                    text = if (isClick) {
                        scrollView.removeView(mainLayout)
                        scrollView.addView(settingLayout)
                        "Close"
                    } else {
                        scrollView.removeView(settingLayout)
                        scrollView.addView(mainLayout)
                        "Setting"
                    }
                }
            })
        }

        root.addView(ivIcon)
        menuLayout.addView(header)
        scrollView.addView(mainLayout)
        menuLayout.addView(scrollView)
        navigation.addView(setting)
        navigation.addView(minimize)
        menuLayout.addView(navigation)
        root.addView(menuLayout)
        floatingLayout.addView(root)
    }

    private fun setMenuList(layout: LinearLayout, features: Array<String>): LinearLayout {
        var id: Int
        var nextId = 0
        var resultLayout = layout

        for (i in features.indices) {
            var menu = features[i]
            var menuSplit = menu.split("_")
            resultLayout = layout
            var toggleOn = false

            if (TextUtils.isDigitsOnly(menuSplit[0]) || menuSplit[0].matches(Regex("-[0-9]*"))) {
                id = menuSplit[0].toInt()
                menu = menu.replaceFirst(menuSplit[0] + "_", "")
                nextId++
            } else {
                id = i - nextId
            }

            if (menu.contains("CollapseAdd_")) {
                resultLayout = collapseLayout
                menu = menu.replaceFirst("CollapseAdd_", "")
            }

            if (menu.contains("True_")) {
                toggleOn = true
                menu = menu.replaceFirst("True_", "")
            }

            menuSplit = menu.split("_")

            when (menuSplit[0]) {
                "Toggle" -> resultLayout.addView(toggle(id, menuSplit[1], toggleOn))
                "SeekBar" -> resultLayout.addView(
                    seekBar(
                        id,
                        menuSplit[1],
                        menuSplit[2].toInt(),
                        menuSplit[3].toInt(),
                    )
                )

                "Button" -> {
                    resultLayout.addView(button(id, menuSplit[1], ""))
                }

                "ButtonLink" -> {
                    resultLayout.addView(button(0, menuSplit[1], menuSplit[2]))
                    nextId++
                }

                "Spinner" -> {
                    resultLayout.addView(text(menuSplit[1]))
                    resultLayout.addView(spinner(id, menuSplit[2]))
                }

                "InputValue" -> {
                    if (menuSplit.size == 2) {
                        resultLayout.addView(field(id, menuSplit[1], true, 0))
                    } else if (menuSplit.size == 3) {
                        resultLayout.addView(field(id, menuSplit[1], true, menuSplit[2].toInt()))
                    }
                }

                "Collapse" -> {
                    collapse(resultLayout, menuSplit[1])
                    nextId++
                }

                "RichTextView" -> {
                    resultLayout.addView(text(menuSplit[1]))
                    nextId++
                }

                "InputText" -> resultLayout.addView(field(id, menuSplit[1], false, 0))
                "RadioButton" -> resultLayout.addView(radio(id, menuSplit[1], menuSplit[2]))
            }
        }

        return resultLayout
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun toggle(id: Int, name: String, value: Boolean): Switch {
        val colorList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                Color.BLUE,
                getColor(R.color.md_theme_primary),
                getColor(R.color.md_theme_outline)
            )
        )

        val switch = Switch(this).apply {
            thumbDrawable.setTintList(colorList)
            trackDrawable.setTintList(colorList)
            text = name
            setTextColor(getColor(R.color.md_theme_onPrimary))
            setPadding(dp(10f), dp(2f), dp(7f), dp(2f))
            val newValue = getBool(id)
            isChecked = if (newValue) newValue else value
            typeface = Typeface.MONOSPACE

            setOnCheckedChangeListener { _, p1 ->
                setBool(id, p1)

                when (id) {
                    -1 -> {
                        isSave = p1

                        if (!p1) {
                            MenuPreference.getSharedPreferences().edit().clear().apply()
                        }
                    }

                    -2 -> {
                        isAutoSize = p1
                        scrollView.layoutParams = if (p1) expandParams else menuParams
                    }
                }
            }
        }

        return switch
    }

    private fun seekBar(id: Int, name: String, minValue: Int, maxValue: Int): View {
        val value = getInt(id, minValue)
        val layout = LinearLayout(this).apply {
            setPadding(dp(10f), dp(2f), dp(10f), dp(2f))
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val textView = TextView(this).apply {
            text = Html.fromHtml(
                "$name: <font color=#ec4c56> ${value ?: minValue}</font>"
            )
            setTextColor(getColor(R.color.md_theme_onPrimary))
            typeface = Typeface.MONOSPACE
        }

        val seekBar = SeekBar(this).apply {
            setPadding(dp(7f), dp(2f), dp(7f), dp(2f))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) min = minValue
            max = maxValue
            thumb.setColorFilter(getColor(R.color.md_theme_primary), PorterDuff.Mode.SRC_ATOP)
            progressDrawable.setColorFilter(
                getColor(R.color.md_theme_primary),
                PorterDuff.Mode.SRC_ATOP
            )
            progress = value ?: minValue

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    textView.text = Html.fromHtml("$name: <font color=#ec4c56> $p1</font>")
                    setInt(id, p1)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }

        layout.addView(textView)
        layout.addView(seekBar)
        return layout
    }

    @SuppressLint("HardwareIds", "SetJavaScriptEnabled", "SetTextI18n", "WrongConstant")
    private fun button(id: Int, name: String, url: String): View {
        val lpButton = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(dp(10f), dp(2f), dp(10f), dp(2f))
        }

        val button = Button(this).apply {
            layoutParams = lpButton
            setTextColor(getColor(R.color.md_theme_onPrimary))
            isAllCaps = true
            text = Html.fromHtml(name)
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                GradientDrawable().apply {
                    setColor(getColor(R.color.md_theme_surface))
                },
                null
            )
            typeface = Typeface.MONOSPACE

            setOnClickListener {
                if (url.isNotEmpty()) {
                    Intent(Intent.ACTION_VIEW).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = Uri.parse(url)
                        startActivity(this)
                    }
                } else {
                    setBool(id, false)

                    if (id == -3) {
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }
        }

        return button
    }

    private fun spinner(id: Int, value: String): View {
        val list = value.split(",")

        val lpSpinner = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(dp(10f), dp(2f), dp(10f), dp(2f))
        }

        val layout = LinearLayout(this).apply {
            layoutParams = lpSpinner
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.md_theme_surface))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val spinner = Spinner(this).apply {
            setPadding(dp(5f), dp(5f), dp(5f), dp(5f))
            background.setColorFilter(getColor(R.color.md_theme_primary), PorterDuff.Mode.SRC_ATOP)
            this.adapter = adapter
            setSelection(getInt(id) ?: 0)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    (p0?.getChildAt(0) as TextView).apply {
                        setTextColor(getColor(R.color.md_theme_onPrimary))
                        typeface = Typeface.MONOSPACE
                    }

                    setInt(id, p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    return
                }
            }
        }

        layout.addView(spinner)
        return layout
    }

    @SuppressLint("SetTextI18n", "WrongConstant")
    private fun field(id: Int, name: String, isNum: Boolean, maxValue: Int): View {
        var valueInt = 0
        var valueStr = ""
        val buttonLayout = LinearLayout(this)

        val lpButton = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(dp(10f), dp(2f), dp(10f), dp(2f))
        }

        val button = Button(this).apply {
            layoutParams = lpButton
            isAllCaps = false

            text = if (isNum) {
                valueInt = getInt(id) ?: 0
                Html.fromHtml("$name: <font color=#ec4c56>$valueInt</font>")
            } else {
                valueStr = getStr(id) ?: ""
                Html.fromHtml("$name: <font color=#ec4c56>'$valueStr'</font>")
            }

            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                GradientDrawable().apply {
                    setColor(getColor(R.color.md_theme_surface))
                },
                null
            )
            setTextColor(getColor(R.color.md_theme_onPrimary))
            typeface = Typeface.MONOSPACE
        }

        button.setOnClickListener {
            val alert = AlertDialog.Builder(this).create()
            Objects.requireNonNull(alert.window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT))
            val inputMgr = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val text = "Tap OK to apply changes. Tap outside to cancel"

            alert.setOnCancelListener {
                inputMgr.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }

            val alertLayout = LinearLayout(this).apply {
                setPadding(dp(5f), dp(5f), dp(5f), dp(5f))
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(getColor(R.color.md_theme_background))
            }

            val textView = TextView(this).apply {
                if (maxValue > 0) {
                    this.text = "$text\nMax value: $maxValue"
                } else {
                    this.text = text
                }

                setTextColor(getColor(R.color.md_theme_onPrimary))
                typeface = Typeface.MONOSPACE
            }

            val editText = EditText(this).apply {
                width = dp(300f)
                setTextColor(getColor(R.color.md_theme_onPrimary))
                typeface = Typeface.MONOSPACE
                requestFocus()

                if (isNum) {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    keyListener = DigitsKeyListener.getInstance("0123456789-")
                    setText(valueInt.toString())
                } else {
                    setText(valueStr)
                }
            }

            editText.setOnFocusChangeListener { _, p1 ->
                if (p1) {
                    inputMgr.toggleSoftInput(
                        InputMethodManager.SHOW_IMPLICIT,
                        InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                } else {
                    inputMgr.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                }
            }

            val btnApply = Button(this).apply {
                background = RippleDrawable(
                    ColorStateList.valueOf(Color.parseColor("#33000000")),
                    GradientDrawable().apply {
                        setColor(getColor(R.color.md_theme_surface))
                    },
                    null
                )
                setTextColor(getColor(R.color.md_theme_onPrimary))
                this.text = "OK"
            }

            btnApply.setOnClickListener {
                if (isNum) {
                    var value: Int

                    value = try {
                        (if (TextUtils.isEmpty(editText.text)) "0" else editText.text.toString()).toInt()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                        214783640
                    }

                    if (maxValue in 1..<value) {
                        value = maxValue
                    }

                    valueInt = value
                    button.text = Html.fromHtml("$name: <font color=#ec4c56>$value</font>")
                    setInt(id, value)
                } else {
                    val value = editText.text.toString()
                    valueStr = value
                    button.text = Html.fromHtml("$name: <font color=#ec4c56>'$value'</font>")
                    setStr(id, value)
                }

                alert.dismiss()
                editText.isFocusable = false
            }

            alertLayout.addView(textView)
            alertLayout.addView(editText)
            alertLayout.addView(btnApply)
            alert.setView(alertLayout)
            alert.show()
        }

        buttonLayout.addView(button)
        return buttonLayout
    }

    private fun radio(id: Int, name: String, value: String): View {
        val checkName = name != "null"
        val index = getInt(id, if (checkName) 1 else 0) ?: 0
        val list = value.split(",")

        val textView = TextView(this).apply {
            text = Html.fromHtml("$name: <font color=#ec4c56>${list[0]}</font>")
            setTextColor(getColor(R.color.md_theme_onPrimary))
            typeface = Typeface.MONOSPACE
        }

        val radioGroup = RadioGroup(this).apply {
            setPadding(dp(10f), dp(2f), dp(10f), dp(2f))
            orientation = LinearLayout.VERTICAL

            if (checkName) {
                addView(textView)
            }
        }

        for (i in list.indices) {
            val text = list[i]

            val radioBtn = RadioButton(this).apply {
                setTextColor(getColor(R.color.md_theme_outline))
                buttonTintList = ColorStateList.valueOf(getColor(R.color.md_theme_primary))
                this.text = text
                typeface = Typeface.MONOSPACE

                setOnClickListener {
                    textView.text = Html.fromHtml("$name: <font color=#ec4c56>$text</font>")
                    val ind = radioGroup.indexOfChild(this)
                    setInt(id, ind)
                }

                setOnCheckedChangeListener { _, b ->
                    if (b) {
                        setTextColor(getColor(R.color.md_theme_onPrimary))
                    } else {
                        setTextColor(getColor(R.color.md_theme_outline))
                    }
                }
            }

            radioGroup.addView(radioBtn)
        }

        if (index > 0) {
            textView.text =
                Html.fromHtml("$name: <font color=#ec4c56>${list[if (checkName) index - 1 else index]}</font>")
            val button = radioGroup.getChildAt(index) as RadioButton
            button.setTextColor(getColor(R.color.md_theme_onPrimary))
            button.isChecked = true
        } else {
            (radioGroup.getChildAt(if (checkName) 1 else 0) as RadioButton).isChecked = true
        }

        return radioGroup
    }

    @SuppressLint("SetTextI18n")
    private fun collapse(layout: LinearLayout, name: String) {
        val lpHeader = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(2f)
            bottomMargin = dp(2f)
        }

        val headerLayout = LinearLayout(this).apply {
            layoutParams = lpHeader
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.VERTICAL
        }

        val childLayout = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(0f), dp(2f), dp(0f), dp(2f))
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.md_theme_background))
            visibility = View.GONE
            collapseLayout = this
        }

        val textView = TextView(this).apply {
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                GradientDrawable().apply {
                    setColor(getColor(R.color.md_theme_surface))
                },
                null
            )
            text = Html.fromHtml("<font color=#ec4c56>▷</font> $name")
            setTextColor(getColor(R.color.md_theme_onPrimary))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dp(10f), dp(5f), dp(5f), dp(5f))
        }

        textView.setOnClickListener(object : View.OnClickListener {
            var isCheck = false

            override fun onClick(p0: View?) {
                isCheck = !isCheck

                if (isCheck) {
                    childLayout.visibility = View.VISIBLE
                    textView.text = Html.fromHtml("<font color=#ec4c56>▽</font> $name")
                } else {
                    childLayout.visibility = View.GONE
                    textView.text = Html.fromHtml("<font color=#ec4c56>▷</font> $name")
                }
            }
        })

        headerLayout.addView(textView)
        headerLayout.addView(childLayout)
        layout.addView(headerLayout)
    }

    private fun text(value: String): View {
        return TextView(this).apply {
            text = Html.fromHtml(value)
            setTextColor(getColor(R.color.md_theme_onPrimary))
            typeface = Typeface.MONOSPACE
            setPadding(dp(10f), dp(2f), dp(10f), dp(2f))
        }
    }

    private fun getBool(id: Int, value: Boolean = false): Boolean {
        if (isSave || id < 0) {
            MenuPreference.getBool(id.toString(), value).also {

                when (id) {
                    -1 -> isSave = it
                    -2 -> isAutoSize = it
                }

                return it
            }
        }

        return value
    }

    private fun setBool(id: Int, value: Boolean) {
        MenuPreference.setBool(id.toString(), value)
    }

    private fun getInt(id: Int, value: Int = 0): Int? {
        if (isSave || id < 0) {
            MenuPreference.getInt(id.toString(), value).also {
                return it
            }
        }

        return null
    }

    private fun setInt(id: Int, value: Int) {
        MenuPreference.setInt(id.toString(), value)
    }

    private fun getStr(id: Int, value: String = ""): String? {
        if (isSave || id < 0) {
            MenuPreference.getString(id.toString(), value).also {
                return it
            }
        }

        return null
    }

    private fun setStr(id: Int, value: String) {
        MenuPreference.setString(id.toString(), value)
    }

    companion object {
        const val CHANNEL_ID = "ChannelID"
        const val NOTIFICATION = "Floating Menu"
        const val FOREGROUND_ID = 1
        const val EXTRA_NAME = "extra_name"
        const val WINDOW_X = "window_x"
        const val WINDOW_Y = "window_y"

        var isStarted = false
    }
}