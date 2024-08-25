package com.hfc.netty

import android.util.SparseArray
import androidx.core.util.getOrDefault
import androidx.core.util.size

class SensorCacheManager {
    private val typeToUidsCache = SparseArray<HashSet<Int>>()
    private val uidToTypesCache = SparseArray<HashSet<Int>>()
    var typeUnUseListener: ((type: Int)->Unit)? = null

    fun enable(type: Int, uid: Int) {
        val uids = typeToUidsCache.getOrDefault(type, HashSet())
        val types = uidToTypesCache.getOrDefault(uid, HashSet())
        uids.add(uid)
        typeToUidsCache[type] = uids
        types.add(type)
        uidToTypesCache[uid] = types
        printCache()
    }

    fun disable(type: Int, uid: Int) {
        typeToUidsCache.get(type)?.also {uids->
            uids.remove(uid)
            if (uids.isEmpty()) {
                typeToUidsCache.remove(type)
                typeUnUseListener?.invoke(type)
            }
        }
        uidToTypesCache.get(uid)?.also { types ->
            types.remove(type)
            if (types.isEmpty()) {
                uidToTypesCache.remove(uid)
            }
        }
        printCache()
    }

    fun state(uid: Int) {
        uidToTypesCache.get(uid)?.onEach {
            typeToUidsCache.get(it).also {uids->
                uids.remove(uid)
                if (uids.isEmpty()) {
                    typeToUidsCache.remove(it)
                    typeUnUseListener?.invoke(it)
                }
            }
        }
        uidToTypesCache.remove(uid)
        printCache()
    }

    fun printCache(){
        println("------------------start--------------------")
        println(typeToUidsCache)
        println(uidToTypesCache)
        println("------------------end--------------------")
    }

    fun hasEnable(type: Int): Boolean = typeToUidsCache[type] != null

    fun clear(){
        typeToUidsCache.clear()
        uidToTypesCache.clear()
    }
}