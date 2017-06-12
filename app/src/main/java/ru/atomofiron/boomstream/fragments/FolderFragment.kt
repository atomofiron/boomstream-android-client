package ru.atomofiron.boomstream.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import com.arellomobile.mvp.MvpAppCompatFragment
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.PresenterType
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_folder.*
import ru.atomofiron.boomstream.models.Node

import ru.atomofiron.boomstream.R
import ru.atomofiron.boomstream.activities.MainActivity
import ru.atomofiron.boomstream.adapters.NotesAdapter
import ru.atomofiron.boomstream.mvp.presenters.FolderPresenter
import ru.atomofiron.boomstream.mvp.views.FolderView
import ru.atomofiron.boomstream.snack
import android.provider.MediaStore
import com.github.clans.fab.FloatingActionMenu
import kotlinx.android.synthetic.main.fragment_folder.view.*
import ru.atomofiron.boomstream.I


class FolderFragment : MvpAppCompatFragment(), FolderView, MainActivity.OnBackPressedListener, NotesAdapter.OnFolderClickListener {

    private var c: Int = 0
    private var mainView: View? = null

    companion object {
        const val TAG = "FolderFragment"
        fun getIntent(context: Context): Intent = Intent(context, FolderFragment::class.java)
    }

    @InjectPresenter(type=PresenterType.GLOBAL)
    lateinit var presenter: FolderPresenter
    private lateinit var listAdapter: NotesAdapter

    // Native //

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val view = inflater!!.inflate(R.layout.fragment_folder, container, false)

        val etSearch = view.findViewById(R.id.etSearch) as EditText
        val fab = view.findViewById(R.id.fab) as FloatingActionMenu
        val rvNotesList = view.findViewById(R.id.rvNotesList) as RecyclerView
        val swipeLayout = view.findViewById(R.id.swipeLayout) as SwipeRefreshLayout
        swipeLayout.setOnRefreshListener {
            presenter.onReloadNodes()
        }

        // у com.github.clans.fab.FloatingActionButton нет возможности указать цвет в xml
        fab.menu_item_pick.setColorNormalResId(R.color.colorAccent)
        fab.menu_item_pick.setColorPressedResId(R.color.colorAccent)
        fab.menu_item_record.setColorNormalResId(R.color.colorAccent)
        fab.menu_item_record.setColorPressedResId(R.color.colorAccent)
        // у com.github.clans.fab.FloatingActionButton нет возможности указать векторное изображение
        // при vectorDrawables.useSupportLibrary = true
        fab.menu_item_pick.setImageResource(R.drawable.ic_video_library)
        fab.menu_item_record.setImageResource(R.drawable.ic_shutter)
        fab.menu_item_pick.setOnClickListener {
            fab.close(true)

            requestPickVideo()
        }
        fab.menu_item_record.setOnClickListener {
            fab.close(true)

            requestRecordVideo()
        }

        RxTextView.textChanges(etSearch)
                .map { text -> text.trim() }
                .filter { text -> isResumed && text.isNotEmpty() }
                .subscribe { text -> search(text.toString()) }

        listAdapter = NotesAdapter(LayoutInflater.from(activity), activity.resources)
        listAdapter.onFolderClickListener = this
        listAdapter.onMediaClickListener = activity as MainActivity

        rvNotesList.layoutManager = LinearLayoutManager(activity) as RecyclerView.LayoutManager
        rvNotesList.adapter = listAdapter

        mainView = view
        return view
    }

    private fun requestRecordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) != null)
            activity.startActivityForResult(intent, I.ACTION_VIDEO_CAPTURE)
        else
            fab.snack(R.string.no_apps)
    }

    private fun requestPickVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        if (intent.resolveActivity(activity.packageManager) != null)
            activity.startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_video)), I.ACTION_VIDEO_PICK)
        else
            fab.snack(R.string.no_apps)
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).onBackPressedListener = this
        if (presenter != null) // только для отладки
            presenter.loadNodesIfNecessary()
        else
            I.Log("CLEAN PROJECT")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.search) {
            switchSearch()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed(): Boolean {
        return listAdapter.goUp()
    }

    // Custom //

    private fun switchSearch() {
        showSearch(etSearch.visibility != View.VISIBLE)
    }

    private fun showSearch(show: Boolean) {
        etSearch.visibility = if (show) View.VISIBLE else View.GONE

        listAdapter.setQuery(if (show) etSearch.text.toString() else "")
    }

    fun updateView() {
        tvEmpty.visibility = if (listAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    fun search(query: String) {
        etSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        listAdapter.setQuery(query)

        updateView()
    }

    override fun onFolderClick(code: String) {
        listAdapter.setQuery("")
        presenter.onOpenFolder(code)
    }

    // MainView implementation //

    override fun onNodesLoaded(nodes: List<Node>) {
        listAdapter.setData(nodes as ArrayList<Node>)
        swipeLayout.isRefreshing = false

        updateView()
    }

    override fun onNodesReloading() {
        listAdapter.clearData()
        swipeLayout.isRefreshing = true

        updateView()
    }

    override fun onNodesLoadFail(message: String) {
        swipeLayout.isRefreshing = false

        try {
            fab.snack(getString(message.toInt()))
        } catch (e: Exception) {
            fab.snack(message)
        }
    }

    override fun onOpenFolder(code: String) {
        listAdapter.openFolder(code)

        showSearch(false)
    }

}
