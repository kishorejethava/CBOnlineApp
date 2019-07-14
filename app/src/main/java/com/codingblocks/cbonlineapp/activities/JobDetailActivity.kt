package com.codingblocks.cbonlineapp.activities

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.adapters.CourseDataAdapter
import com.codingblocks.cbonlineapp.database.models.CourseRun
import com.codingblocks.cbonlineapp.extensions.getSpannableSring
import com.codingblocks.cbonlineapp.extensions.observer
import com.codingblocks.cbonlineapp.util.JOB_ID
import com.codingblocks.cbonlineapp.viewmodels.JobDetailViewModel
import com.codingblocks.onlineapi.models.Form
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.item_job.companyLogo
import kotlinx.android.synthetic.main.item_job.companyTv
import kotlinx.android.synthetic.main.item_job.ctcTv
import kotlinx.android.synthetic.main.item_job.deadlinell
import kotlinx.android.synthetic.main.item_job.experienceTv
import kotlinx.android.synthetic.main.item_job.jobTitleTv
import kotlinx.android.synthetic.main.item_job.locationTv
import kotlinx.android.synthetic.main.item_job.postedAgoTv
import kotlinx.android.synthetic.main.item_job.typeTv
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.view.WindowManager
import com.codingblocks.cbonlineapp.extensions.observeOnce
import kotlinx.android.synthetic.main.activity_job_detail.*
import kotlinx.android.synthetic.main.custom_form_dialog.view.*

class JobDetailActivity : AppCompatActivity() {

    private val viewModel by viewModel<JobDetailViewModel>()

    private lateinit var courseDataAdapter: CourseDataAdapter

    lateinit var jobId: String

    val formDialog by lazy {
        AlertDialog.Builder(this).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        jobId = intent.getStringExtra(JOB_ID)

        deadlinell.visibility = View.GONE

        viewModel.fetchJob(jobId)

        courseDataAdapter =
            CourseDataAdapter(
                ArrayList(),
                viewModel.getCourseDao(),
                this,
                viewModel.getCourseWithInstructorDao(),
                "allCourses"
            )

        rvJobCourses.layoutManager = LinearLayoutManager(this)
        rvJobCourses.adapter = courseDataAdapter

        jobDescriptionBtn.setOnClickListener {
            cardJobDescription.isVisible = !cardJobDescription.isVisible
        }
        makeForm(viewModel.formData)
        setUpObservers()
    }

    private fun setUpObservers() {
        viewModel.getJobById(jobId).observeOnce {
            with(it) {
                Picasso.with(this@JobDetailActivity).load(company.logo).into(companyLogo)
                jobTitleTv.text = title
                supportActionBar?.title = title
                companyTv.text = company.name
                postedAgoTv.text = postedOn
                locationTv.text = getSpannableSring("Job Location: ", location)
                experienceTv.text = getSpannableSring("Experience: ", experience)
                typeTv.text = getSpannableSring("Job Type: ", type)
                ctcTv.text = getSpannableSring("CTC: ", ctc)
                jobDescriptionTv.text = description
                companyDescriptionTv.text = company.companyDescription
                eligibleTv.text = "Eligibility:    $eligibility"
                viewModel.getCourses(courseId)
            }
        }

        viewModel.jobCourses.observer(this) {
            courseDataAdapter.setData(it as ArrayList<CourseRun>)
        }

        viewModel.eligibleLiveData.observeOnce {
            if (it) {
                statusTv.text = "Status:  Eligible"
            } else {
                statusTv.setTextColor(resources.getColor(R.color.salmon))
                statusTv.text = "Status:  Not Eligible"
                addResumeBtn.isVisible = false
            }
        }
    }

    private fun makeForm(formData: MutableLiveData<ArrayList<Form>>) {

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val sizeInDP = 8

        val marginInDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeInDP.toFloat(), resources
            .displayMetrics
        ).toInt()
        params.setMargins(marginInDp, marginInDp / 2, marginInDp, marginInDp / 2)
        val formView = layoutInflater.inflate(R.layout.custom_form_dialog, null)
        val formlayout = formView.form
        formData.observer(this) {
            it.forEachIndexed { index, it ->
                if (it.type == "text-field") {
                    val inputLayout = TextInputLayout(
                        this,
                        null,
                        R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox
                    )

                    inputLayout.layoutParams = params
                    inputLayout.setPadding(marginInDp, 0, 0, 0)
                    if (it.required) {
                        if (it.title.length > 30) {
                            val title = TextView(this).apply {
                                text = "${it.title}*"
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                layoutParams = params
                            }
                            formlayout.addView(title)
                        } else {
                            inputLayout.hint = "${it.title}*"
                        }
                    } else {
                        if (it.title.length > 30) {
                            val title = TextView(this).apply {
                                text = it.title
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                layoutParams = params
                            }
                            formlayout.addView(title)
                        } else {
                            inputLayout.hint = it.title
                        }
                    }
                    inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
                    inputLayout.setBoxCornerRadii(20f, 20f, 20f, 20f)
                    val edittext = TextInputEditText(inputLayout.context)
                    edittext.setOnFocusChangeListener { view, b ->
                    }
                    inputLayout.addView(edittext)
                    inputLayout.tag = it.name

                    formlayout.addView(inputLayout)
                } else if (it.type == "radio-group") {
                    val title = TextView(this).apply {
                        text = it.title
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        layoutParams = params
                    }
                    formlayout.addView(title)
                    val optionsArray = it.options?.split(",")

                    val rb = arrayOfNulls<RadioButton>(optionsArray!!.size)
                    val rg = RadioGroup(this) // create the RadioGroup
                    rg.layoutParams = params
                    rg.orientation = RadioGroup.VERTICAL // or RadioGroup.VERTICAL
                    for (i in rb.indices) {
                        rb[i] = RadioButton(this)
                        rb[i]?.text = optionsArray[i]
                        if (i == 0) {
                            rb[i]?.isChecked = true
                        }
                        rg.addView(rb[i])
                    }
                    rg.tag = it.name
                    formlayout.addView(rg)
                }
            }

            formView.okBtn.setOnClickListener { view ->
                val jsonObject = JsonObject()
                it.forEach { form ->
                    if (form.type == "text-field") {
                        val inputLayout = formlayout.findViewWithTag<TextInputLayout>(form.name)
                        if (inputLayout.editText?.text.isNullOrEmpty() && form.required) {
                            inputLayout.isErrorEnabled = true
                            inputLayout.error = "Cannot Be Empty"
                        } else {
                            inputLayout.isErrorEnabled = false
                            inputLayout.error = null
                            jsonObject.addProperty(form.name, inputLayout.editText?.text.toString())
                        }
                    } else if (form.type == "radio-group") {
                        val group = formlayout.findViewWithTag<RadioGroup>(form.name)
//                        jsonObject.addProperty(form.name, findViewById<RadioButton>(group.checkedRadioButtonId).text.toString())
                    }
                }
            }
            formView.cancelBtn.setOnClickListener { view ->
                it.forEach { form ->
                    if (form.type == "text-field") {
                        val inputLayout = formlayout.findViewWithTag<TextInputLayout>(form.name)
                        inputLayout.editText?.setText("")
                    }
                }
            }
        }

        addResumeBtn.setOnClickListener {
            formDialog.apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setView(formView)
                setCancelable(true)
                show()
                window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }
}
