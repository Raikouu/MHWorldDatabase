package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.gatheringhallstudios.mhworlddatabase.AppSettings

import com.gatheringhallstudios.mhworlddatabase.data.AppDatabase
import com.gatheringhallstudios.mhworlddatabase.data.MHWDatabase
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.data.types.DataType
import kotlinx.coroutines.*

/**
 * Created by Carlos on 3/22/2018.
 */

class UserEquipmentSetListViewModel(application: Application) : AndroidViewModel(application) {
    private val decorationDao = MHWDatabase.getDatabase(application).decorationDao()
    private val charmDao = MHWDatabase.getDatabase(application).charmDao()
    private val weaponDao = MHWDatabase.getDatabase(application).weaponDao()
    private val armorDao = MHWDatabase.getDatabase(application).armorDao()
    private val appDao = AppDatabase.getAppDataBase(application)!!.userEquipmentSetDao()

    val userEquipmentSetIds = MutableLiveData<MutableList<UserEquipmentSetIds>>()
    val userEquipmentSets = MutableLiveData<MutableList<UserEquipmentSet>>()

    fun createEquipmentSet(): UserEquipmentSet {
        val newId = appDao.createUserEquipmentSet("New Set")
        return convertEquipmentSetIdToEquipmentSet(appDao.loadUserEquipmentSetIds(newId.toInt()))
    }

    fun getEquipmentSetList() {
        GlobalScope.launch(Dispatchers.Main) {
            val equipmentSetIds = withContext(Dispatchers.IO) {
                //                                appDao.createUserEquipmentSet("test")
////                                appDao.createUserEquipmentEquipment(629, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(630, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(631, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(632, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(633, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(634, DataType.ARMOR, 1)
//                appDao.deleteUserEquipmentEquipment(526, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(526, DataType.ARMOR, 1)
//                                appDao.createUserEquipmentEquipment(1, DataType.CHARM, 1)
//                appDao.createUserEquipmentDecoration(1, 526, DataType.ARMOR, 94, 1)
//                appDao.createUserEquipmentEquipment(634, DataType.ARMOR, 1)
//                appDao.deleteUserEquipmentEquipment(634, DataType.ARMOR, 1)
//                appDao.createUserEquipmentEquipment(200, DataType.WEAPON, 1)
//                appDao.deleteUserEquipmentEquipment(630, DataType.ARMOR, 1)
//                appDao.deleteUserEquipmentEquipment(631, DataType.ARMOR, 1)
//                appDao.deleteUserEquipmentEquipment(633, DataType.ARMOR, 1)
//                appDao.deleteUserEquipmentSet(2)
//                appDao.deleteUserEquipmentSet(3)
//                appDao.deleteUserEquipmentSet(4)
//                appDao.deleteUserEquipmentSet(5)
                appDao.loadUserEquipmentSetIds()
            }

            val equipmentSets = withContext(Dispatchers.IO) {
                val deferred = equipmentSetIds.map {
                    async {
                        println("Doing on " + Thread.currentThread().name)
                        convertEquipmentSetIdToEquipmentSet(it)
                    }
                }

                deferred.map { it.await() }
            }

            userEquipmentSets.value = equipmentSets.toMutableList()
            userEquipmentSetIds.value = equipmentSetIds.toMutableList()
        }
    }

    fun getEquipmentSet(equipmentSetId: Int): UserEquipmentSet {
        return runBlocking {
            val equipmentSetIds = withContext(Dispatchers.IO) {
                appDao.loadUserEquipmentSetIds(equipmentSetId)
            }

            val equipmentSet = withContext(Dispatchers.IO) {
                val deferred = async {
                    println("Doing on " + Thread.currentThread().name)
                    convertEquipmentSetIdToEquipmentSet(equipmentSetIds)
                }

                deferred.await()
            }

            val buffer = userEquipmentSets.value
            if (buffer != null) {
                //Update user equipment sets collection in memory if it exists
                val index = buffer.indexOfFirst {
                    it.id == equipmentSet.id
                }

                if (index != -1) {
                    buffer[index] = equipmentSet
                }

                userEquipmentSets.value = buffer
            }

            equipmentSet
        }
    }

    private fun convertEquipmentSetIdToEquipmentSet(userEquipmentSetIds: UserEquipmentSetIds): UserEquipmentSet {
        val userEquipment = mutableListOf<UserEquipment>()
        userEquipmentSetIds.equipmentIds.forEach { userEquipmentId ->
            when (userEquipmentId.dataType) {
                DataType.ARMOR -> {
                    val decorations = userEquipmentId.decorationIds.map { decorationIds ->
                        UserDecoration(decoration = decorationDao.loadDecorationSync(AppSettings.dataLocale, decorationIds.decorationId), slotNumber = decorationIds.slotNumber)
                    }

                    userEquipment.add(UserArmorPiece(armor = armorDao.loadArmorFullSync(AppSettings.dataLocale, userEquipmentId.dataId),
                            decorations = decorations))
                }
                DataType.WEAPON -> {
                    val decorations = userEquipmentId.decorationIds.map { decorationIds ->
                        UserDecoration(decoration = decorationDao.loadDecorationSync(AppSettings.dataLocale, decorationIds.decorationId), slotNumber = decorationIds.slotNumber)
                    }

                    userEquipment.add(UserWeapon(weapon = weaponDao.loadWeaponFullSync(AppSettings.dataLocale, userEquipmentId.dataId),
                            decorations = decorations))
                }
                DataType.CHARM -> {
                    userEquipment.add(UserCharm(charmDao.loadCharmFullSync(AppSettings.dataLocale, userEquipmentId.dataId)))
                }
                else -> {
                } //Shouldn't happen, so ignore
            }
        }

        return {
            val set = UserEquipmentSet(userEquipmentSetIds.id, userEquipmentSetIds.name, userEquipment)
            set.defense_base = calculateDefenseBase(userEquipment)
            set.defense_max = calculateDefenseMax(userEquipment)
            set.defense_augment_max = calculateDefenseAugMax(userEquipment)
            set.fireDefense = calculateFireDefense(userEquipment)
            set.waterDefense = calculateWaterDefense(userEquipment)
            set.thunderDefense = calculateThunderDefense(userEquipment)
            set.iceDefense = calculateIceDefense(userEquipment)
            set.dragonDefense = calculateDragonDefense(userEquipment)
            set.skills = calculateSkillLevels(userEquipment)
            set.setBonuses = calculateSetBonuses(userEquipment)

            set
        }()
    }

    private fun calculateSetBonuses(userEquipment: List<UserEquipment>): MutableMap<String, List<ArmorSetBonus>> {
        val setBonuses = mutableMapOf<String, List<ArmorSetBonus>>()
        val setsPresent = userEquipment
                .filter { it.type() == DataType.ARMOR }
                .groupBy { (it as UserArmorPiece).armor.armor.armorset_id }

        //Set Bonus breakpoints are at 2-pieces, 3-pieces, and 4-pieces
        setsPresent.forEach {
            if (it.value.isNotEmpty()) {
                val userArmorPiece = (it.value.first() as UserArmorPiece)
                val activeSetBonuses = userArmorPiece.armor.setBonuses
                        .filter { setBonus -> it.value.size >= setBonus.required }

                if (activeSetBonuses.isNotEmpty()) {
                    setBonuses[activeSetBonuses.first().name!!] = activeSetBonuses
                }
            }
        }

        return setBonuses
    }

    private fun calculateDefenseBase(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.defense_base
            } else if (item.type() == DataType.WEAPON) {
                (item as UserWeapon).weapon.weapon.defense
            } else 0
        }
    }

    private fun calculateDefenseMax(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.defense_max
            } else if (item.type() == DataType.WEAPON) {
                (item as UserWeapon).weapon.weapon.defense
            } else 0
        }
    }

    private fun calculateDefenseAugMax(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.defense_augment_max
            } else if (item.type() == DataType.WEAPON) {
                (item as UserWeapon).weapon.weapon.defense
            } else 0
        }
    }

    private fun calculateFireDefense(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.fire
            } else 0
        }
    }

    private fun calculateWaterDefense(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.water
            } else 0
        }
    }

    private fun calculateThunderDefense(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.thunder
            } else 0
        }
    }

    private fun calculateIceDefense(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.ice
            } else 0
        }
    }

    private fun calculateDragonDefense(userEquipment: List<UserEquipment>): Int {
        return userEquipment.sumBy { item ->
            if (item.type() == DataType.ARMOR) {
                (item as UserArmorPiece).armor.armor.dragon
            } else 0
        }
    }

    private fun calculateSkillLevels(userEquipment: List<UserEquipment>): MutableMap<Int, SkillLevel> {
        val skillLevels = mutableMapOf<Int, SkillLevel>()
        userEquipment.forEach { item ->
            val providedSkills = mutableListOf<SkillLevel>()
            when (item.type()) {
                DataType.ARMOR -> {
                    (item as UserArmorPiece).decorations.forEach { userDecoration ->
                        val skillLevel = SkillLevel(1) //Decorations always only give 1 skill point
                        skillLevel.skillTree = userDecoration.decoration.skillTree
                        providedSkills.add(skillLevel)
                    }

                    providedSkills.addAll(item.armor.skills)
                }

                DataType.WEAPON -> {
                    (item as UserWeapon).decorations.forEach { userDecoration ->
                        val skillLevel = SkillLevel(1) //Decorations always only give 1 skill point
                        skillLevel.skillTree = userDecoration.decoration.skillTree
                        providedSkills.add(skillLevel)
                    }
                }
                DataType.CHARM -> {
                    providedSkills.addAll((item as UserCharm).charm.skills)
                }
                else -> {
                }
            }

            providedSkills.forEach {
                if (skillLevels.containsKey(it.skillTree.id)) {
                    val level = skillLevels[it.skillTree.id]!!.level + it.level
                    val skillLevel = SkillLevel(level)
                    skillLevel.skillTree = it.skillTree
                    skillLevels[it.skillTree.id] = skillLevel
                } else {
                    skillLevels[it.skillTree.id] = it
                }
            }
        }

        return skillLevels
    }
}
