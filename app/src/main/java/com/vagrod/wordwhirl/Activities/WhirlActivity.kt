package com.vagrod.wordwhirl.Activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.vagrod.wordwhirl.*
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataClasses.*
import com.vagrod.wordwhirl.Helpers.OnSwipeTouchListener
import kotlin.random.Random

class WhirlActivity : AppCompatActivity() {
    private lateinit var dataAdapter : DataAdapter
    private lateinit var playlist : MutableList<PlaylistPair>

    private lateinit var options : WhirlOptions
    private var groupInfo : GroupData? = null
    private var wordsData : GroupWords? = null
    private var card : ConstraintLayout? = null
    private var currentPair: PlaylistPair? = null
    private lateinit var groupId : String

    private val fadeDuration : Int = 250
    private var showingBack : Boolean = false

    companion object {
        val GLOBAL_WHIRL: String = "global"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whirl)

        dataAdapter = DataAdapter(this)

        if (dataAdapter.getIsDarkMode()){
            findViewById<LinearLayout>(R.id.bg_holder).background = getDrawable(R.drawable.ic_receipt_dark_24dp)
        } else {
            findViewById<LinearLayout>(R.id.bg_holder).background = getDrawable(R.drawable.ic_receipt_light_24dp)
        }

        card = findViewById(R.id.layout)

        card?.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                nextWord()
            }
            override fun onSwipeRight() {
                prevWord()
            }
            override fun onSwipeTop() {
                flipCard()
            }
            override fun onSwipeBottom() {
                flipCard()
            }
        })

        groupId = intent.getStringExtra("groupId") ?: ""

        val optionsData = intent.getStringExtra("options")

        options = dataAdapter.deserializeWhirlOptions(optionsData ?: "") ?: WhirlOptions(loop = false, flipAll = false, flipOnRandom = false, randomize = false, lastN = -1)

        if(groupId != GLOBAL_WHIRL) {
            groupInfo = dataAdapter.getGroupInfo(groupId)
            wordsData = dataAdapter.getGroupWords(groupId)
        } else {
            groupInfo = GroupData("global", getString(R.string.global_whirl), options.lastN, true)
            wordsData = dataAdapter.getRandomGroupWords(options.lastN)
        }

        playlist = mutableListOf()

        buildWordsPlaylist()
        currentPair = playlist.first()

        updateCaption()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, CardFrontFragment(currentPair?.first))
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.whirl_card_menu, menu)

        if(groupId == GLOBAL_WHIRL){
            // Need to disable editing

            menu.findItem(R.id.action_edit_word).isVisible = false
        }

        if(!options.randomize){
            menu.findItem(R.id.action_shuffle).isVisible = false
        }

        return true
    }

    private fun buildWordsPlaylist(){
        playlist.clear()

        var data : MutableList<WordsPair> = wordsData?.words?.toMutableList() ?: mutableListOf()

        if (options.lastN > 0 && data.count() > 0){
            data = data.takeLast(options.lastN).toMutableList()
        }

        if(options.randomize)
            data.shuffle()

        data.forEach{
            if (options.flipAll && !it.noflip)
                playlist.add(PlaylistPair(it, it.second, it.first, isFlipped = true))
            else {
                if(options.flipOnRandom && !it.noflip){
                    if(Random.nextBoolean())
                        playlist.add(PlaylistPair(it, it.second, it.first, isFlipped = true))
                    else
                        playlist.add(PlaylistPair(it, it.first, it.second, isFlipped = false))
                } else
                    playlist.add(PlaylistPair(it, it.first, it.second, isFlipped = false))
            }
        }
    }

    private fun prevWord(){
        selectPrevWord()
        switchCard()
    }

    private fun nextWord(){
        selectNextWord()
        switchCard()
    }

    private fun selectNextWord(){
        var ind : Int

        if(currentPair == null)
            ind = -1
        else{
            ind = playlist.indexOf(currentPair as PlaylistPair)

            if (ind < 0)
                return
        }

        val cnt = playlist.count()

        if (ind + 1 > cnt - 1){
            if(options.loop)
                ind = 0
            else {
                val builder = AlertDialog.Builder(this)

                builder.setTitle(R.string.whirl_end_confirm_title)
                builder.setMessage(R.string.whirl_end_confirm_message)

                builder.setPositiveButton(R.string.repeat) { _, _ ->
                    shuffle()
                }

                builder.setNegativeButton(R.string.back) { _, _ ->
                    setResult(Activity.RESULT_CANCELED, Intent())
                    finish()
                }

                builder.show()
                return
            }
        } else {
            ind++
        }

        currentPair = playlist[ind]

        updateCaption()
    }

    private fun selectPrevWord(){
        var ind = playlist.indexOf(currentPair)

        if (ind < 0)
            return

        if (ind - 1 < 0){
            ind = 0
        } else {
            ind--
        }

        currentPair = playlist[ind]

        updateCaption()
    }

    private fun updateCaption(){
        if(currentPair==null)
            return

        val ind = playlist.indexOf(currentPair as PlaylistPair)

        title = """${groupInfo?.name} — ${ind+1}/${playlist.count()}"""
    }

    private fun switchCard(){
        fadeOut()

        Handler().postDelayed({
            showingBack = false
            flipCard(invert = true)
            showingBack = false
            fadeIn()
        }, (fadeDuration /  2 + 10).toLong())
    }

    private fun fadeOut(){
        card?.apply {
            alpha = 1f
            visibility = View.VISIBLE

            animate()
                .alpha(0f)
                .setDuration((fadeDuration /  2).toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        card?.visibility = View.INVISIBLE
                    }
                })
        }
    }

    private fun fadeIn(){
        card?.apply {
            alpha = 0f
            visibility = View.INVISIBLE

            animate()
                .alpha(1f)
                .setDuration((fadeDuration /  2).toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        card?.visibility = View.VISIBLE
                    }
                })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            16908332 -> {
                setResult(Activity.RESULT_CANCELED, Intent())

                this.finish()
                return true
            }

            R.id.action_edit_word -> {
                if(currentPair == null)
                    return true

                if(currentPair != null)
                    editWordPair(currentPair as PlaylistPair)

                return true
            }
            R.id.action_shuffle -> {
                shuffle()
            }
        }

        return false
    }

    private fun shuffle(){
        showingBack = false
        buildWordsPlaylist()
        currentPair = null
        selectNextWord()
        switchCard()
    }

    private fun editWordPair(pair: PlaylistPair) {
        val builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.word_pair_editor, null)
        val wordFirst = dialogView.findViewById<EditText>(R.id.wordeditor_first)
        val wordSecond = dialogView.findViewById<EditText>(R.id.wordeditor_second)
        val noflip = dialogView.findViewById<CheckBox>(R.id.noflip)

        if(pair.isFlipped){
            wordFirst?.setText(pair.second)
            wordSecond?.setText(pair.first)
        } else {
            wordFirst?.setText(pair.first)
            wordSecond?.setText(pair.second)
        }

        noflip?.isChecked = pair.originalPair.noflip

        builder.setTitle(R.string.wordpair_title)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val newPair = WordsPair(wordFirst.text.toString(), wordSecond.text.toString(), noflip.isChecked)

            dataAdapter.updateWordsPair(groupId, pair.originalPair, newPair)
            wordsData = dataAdapter.getGroupWords(groupId)

            val plPair = playlist.find { it.first == pair.first && it.second == pair.second }
            val ind = playlist.indexOf(plPair)
            val newFirst = if(pair.isFlipped) newPair.second else newPair.first
            val newSecond = if(pair.isFlipped) newPair.first else newPair.second
            val newPlPair = PlaylistPair(newPair, newFirst, newSecond, pair.isFlipped)

            playlist.removeAt(ind)
            playlist.add(ind, newPlPair)

            currentPair = newPlPair

            flipCard()
            flipCard()
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }

        builder.setView(dialogView)

        val b = builder.create()

        b.show()
    }

    class CardFrontFragment(private val word : String?) : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ):View {
            val v = inflater.inflate(R.layout.fragment_card_front, container, false)

            v.findViewById<TextView>(R.id.front_label).text = word

            return v
        }
    }

    class CardBackFragment(private val word : String?) : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ):View {
            val v = inflater.inflate(R.layout.fragment_card_back, container, false)

            v.findViewById<TextView>(R.id.back_label).text = word

            return v
        }
    }

    private fun flipCard(invert : Boolean  = false) {
        showingBack = !showingBack

        val frag : Fragment

        if (showingBack){
            val w : String?
            if (invert)
                w = currentPair?.first
            else
                w = currentPair?.second

            frag = CardBackFragment(w)
        }
        else{
            val w : String?
            if (invert)
                w = currentPair?.second
            else
                w = currentPair?.first

            frag = CardFrontFragment(w)
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.animator.card_flip_right_in,
                R.animator.card_flip_right_out,
                R.animator.card_flip_left_in,
                R.animator.card_flip_left_out
            )
            .replace(R.id.container, frag)
            .commit()
    }
}
