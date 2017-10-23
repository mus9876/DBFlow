package com.raizlabs.android.dbflow.sql.queriable

import com.raizlabs.android.dbflow.structure.database.FlowCursor

/**
 * Description:
 */
class SingleKeyCacheableListModelLoader<TModel>(tModelClass: Class<TModel>)
    : CacheableListModelLoader<TModel>(tModelClass) {

    override fun convertToData(cursor: FlowCursor, data: MutableList<TModel>?): MutableList<TModel> {
        val _data = data ?: arrayListOf()
        var cacheValue: Any
        // Ensure that we aren't iterating over this cursor concurrently from different threads
        if (cursor.moveToFirst()) {
            do {
                cacheValue = modelAdapter.getCachingColumnValueFromCursor(cursor)
                var model: TModel? = modelCache.get(cacheValue)
                if (model != null) {
                    modelAdapter.reloadRelationships(model, cursor)
                } else {
                    model = modelAdapter.newInstance()
                    modelAdapter.loadFromCursor(cursor, model!!)
                    modelCache.addModel(cacheValue, model)
                }
                _data.add(model)
            } while (cursor.moveToNext())
        }
        return _data
    }

}
