package com.gatheringhallstudios.mhworlddatabase.features.monsters.detail

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.util.pager.BasePagerFragment
import com.gatheringhallstudios.mhworlddatabase.data.models.Monster
import com.gatheringhallstudios.mhworlddatabase.data.types.Rank
import com.gatheringhallstudios.mhworlddatabase.features.bookmarks.BookmarksFeature
import com.gatheringhallstudios.mhworlddatabase.setActivityTitle
import com.gatheringhallstudios.mhworlddatabase.util.getDrawableCompat
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_generic_pager.view.*

/**
 * Monster detail Hub. Displays information for a single monster.
 * All data is displayed in separate tabs.
 */

class MonsterDetailPagerFragment : BasePagerFragment() {
    companion object {
        const val ARG_MONSTER_ID = "MONSTER_ID"
    }

    override val tabMode: Int = TabLayout.MODE_SCROLLABLE

    private lateinit var viewModel : MonsterDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_bookmarkable, menu)
        val monsterData = viewModel.monster.value
        if (monsterData != null && BookmarksFeature.isBookmarked(monsterData)) {
            menu.findItem(R.id.action_toggle_bookmark).icon = (context!!.getDrawableCompat(R.drawable.ic_sys_bookmark_on))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Try to handle the bookmarks button onclick here instead of the main activity
        val id = item.itemId
        super.onOptionsItemSelected(item)
        return if (id == R.id.action_toggle_bookmark) {
            BookmarksFeature.toggleBookmark(viewModel.monster.value)
            activity!!.invalidateOptionsMenu()
            true
        } else false
    }

    override fun onAddTabs(tabs: BasePagerFragment.TabAdder) {
        // Retrieve MonsterID from args (required!)
        val args = arguments
        val monsterId = args!!.getInt(ARG_MONSTER_ID)

        // Set up our ViewModel
        viewModel = ViewModelProviders.of(this).get(MonsterDetailViewModel::class.java)
        viewModel.setMonster(monsterId)

        viewModel.monster.observe(this, Observer<Monster> {
            this.setActivityTitle(it?.name)
            //Rerender the menu bar because we are 100% sure we have the monster data now
            activity!!.invalidateOptionsMenu()
        })

        // Now add our tabs
        tabs.addTab(getString(R.string.tab_monsters_detail_summary)) {
            MonsterSummaryFragment()
        }
        tabs.addTab(getString(R.string.tab_monsters_detail_damage)) {
            MonsterDamageFragment()
        }
        tabs.addTab(getString(R.string.tab_monsters_detail_rewards_master_rank)) {
            MonsterRewardFragment.newInstance(Rank.MASTER)
        }
        tabs.addTab(getString(R.string.tab_monsters_detail_rewards_high_rank)) {
            MonsterRewardFragment.newInstance(Rank.HIGH)
        }
        tabs.addTab(getString(R.string.tab_monsters_detail_rewards_low_rank)) {
            MonsterRewardFragment.newInstance(Rank.LOW)
        }
    }
}
