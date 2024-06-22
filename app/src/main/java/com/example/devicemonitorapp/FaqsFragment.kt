package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.devicemonitorapp.databinding.FragmentFaqsBinding
import com.example.devicemonitorapp.databinding.ItemFaqBinding

data class FAQ(val question: String, val answer: String)

class FaqsFragment : Fragment() {

    private var _binding: FragmentFaqsBinding? = null
    private val binding get() = _binding!!

    private lateinit var faqList: List<FAQ>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFaqsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        faqList = listOf(
            FAQ(getString(R.string.faq_question1), getString(R.string.faq_answer1)),
            FAQ(getString(R.string.faq_question2), getString(R.string.faq_answer2)),
            FAQ(getString(R.string.faq_question3), getString(R.string.faq_answer3)),
            FAQ(getString(R.string.faq_question4), getString(R.string.faq_answer4)),
            FAQ(getString(R.string.faq_question5), getString(R.string.faq_answer5)),
            FAQ(getString(R.string.faq_question6), getString(R.string.faq_answer6)),
            FAQ(getString(R.string.faq_question7), getString(R.string.faq_answer7)),
            FAQ(getString(R.string.faq_question8), getString(R.string.faq_answer8)),
            FAQ(getString(R.string.faq_question9), getString(R.string.faq_answer9))
        )

        // Check and set the background drawable based on the dark mode setting
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night1) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.pic_one) // Replace with your day mode drawable
        }

        val adapter = FAQAdapter(faqList)
        binding.faqRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.faqRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FAQAdapter(private val faqList: List<FAQ>) : RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
            val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FAQViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
            holder.bind(faqList[position])
        }

        override fun getItemCount(): Int = faqList.size

        inner class FAQViewHolder(private val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(faq: FAQ) {
                binding.questionTextView.text = faq.question
                binding.answerTextView.text = faq.answer
                binding.questionTextView.setOnClickListener {
                    if (binding.answerTextView.visibility == View.GONE) {
                        binding.answerTextView.visibility = View.VISIBLE
                    } else {
                        binding.answerTextView.visibility = View.GONE
                    }
                }
            }
        }
    }
}

