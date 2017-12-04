package com.raizlabs.dbflow5.adapter

import android.content.ContentValues
import android.database.sqlite.SQLiteStatement
import com.raizlabs.dbflow5.adapter.saveable.ListModelSaver
import com.raizlabs.dbflow5.adapter.saveable.ModelSaver
import com.raizlabs.dbflow5.annotation.ConflictAction
import com.raizlabs.dbflow5.annotation.ForeignKey
import com.raizlabs.dbflow5.annotation.PrimaryKey
import com.raizlabs.dbflow5.annotation.Table
import com.raizlabs.dbflow5.config.DatabaseDefinition
import com.raizlabs.dbflow5.database.DatabaseStatement
import com.raizlabs.dbflow5.database.DatabaseWrapper
import com.raizlabs.dbflow5.database.FlowCursor
import com.raizlabs.dbflow5.query.property.IProperty
import com.raizlabs.dbflow5.query.property.Property
import com.raizlabs.dbflow5.structure.InvalidDBConfiguration

/**
 * Description: Used for generated classes from the combination of [Table] and [Model].
 */
abstract class ModelAdapter<T : Any>(databaseDefinition: DatabaseDefinition)
    : InstanceAdapter<T>(databaseDefinition), InternalAdapter<T> {

    private var insertStatement: DatabaseStatement? = null
    private var compiledStatement: DatabaseStatement? = null
    private var updateStatement: DatabaseStatement? = null
    private var deleteStatement: DatabaseStatement? = null

    private var _modelSaver: ModelSaver<T>? = null

    val listModelSaver: ListModelSaver<T> by lazy { createListModelSaver() }

    /**
     * @return The autoincrement column name for the [PrimaryKey.autoincrement]
     * if it has the field. This method is overridden when its specified for the [T]
     */
    open val autoIncrementingColumnName: String
        get() = throw InvalidDBConfiguration("This method may have been called in error." +
            " The model class $table must contain an autoincrementing" +
            " or single int/long primary key (if used in a ModelCache, this method may be called)")

    /**
     * @return The query used to create this table.
     */
    abstract val creationQuery: String

    /**
     * @return An array of column properties, in order of declaration.
     */
    abstract val allColumnProperties: Array<IProperty<*>>

    /**
     * @return The query used to insert a model using a [SQLiteStatement]
     */
    protected open val insertStatementQuery: String
        get() = compiledStatementQuery

    /**
     * @return The normal query used in saving a model if we use a [SQLiteStatement].
     */
    protected abstract val compiledStatementQuery: String

    protected abstract val updateStatementQuery: String

    protected abstract val deleteStatementQuery: String

    /**
     * @return The conflict algorithm to use when updating a row in this table.
     */
    open val updateOnConflictAction: ConflictAction
        get() = ConflictAction.ABORT

    /**
     * @return The conflict algorithm to use when inserting a row in this table.
     */
    open val insertOnConflictAction: ConflictAction
        get() = ConflictAction.ABORT

    init {
        tableConfig?.modelSaver?.let { modelSaver ->
            modelSaver.modelAdapter = this
            _modelSaver = modelSaver
        }
    }

    fun closeInsertStatement() {
        insertStatement?.close()
        insertStatement = null
    }

    fun closeUpdateStatement() {
        updateStatement?.close()
        updateStatement = null
    }

    fun closeDeleteStatement() {
        deleteStatement?.close()
        deleteStatement = null
    }

    /**
     * @param databaseWrapper The database used to do an insert statement.
     * @return a new compiled [DatabaseStatement] representing insert. Not cached, always generated.
     * To bind values use [.bindToInsertStatement].
     */
    fun getInsertStatement(databaseWrapper: DatabaseWrapper): DatabaseStatement =
        databaseWrapper.compileStatement(insertStatementQuery)

    /**
     * @param databaseWrapper The database used to do an update statement.
     * @return a new compiled [DatabaseStatement] representing update. Not cached, always generated.
     * To bind values use [.bindToUpdateStatement].
     */
    fun getUpdateStatement(databaseWrapper: DatabaseWrapper): DatabaseStatement =
        databaseWrapper.compileStatement(updateStatementQuery)

    /**
     * @param databaseWrapper The database used to do a delete statement.
     * @return a new compiled [DatabaseStatement] representing delete. Not cached, always generated.
     * To bind values use [.bindToDeleteStatement].
     */
    fun getDeleteStatement(databaseWrapper: DatabaseWrapper): DatabaseStatement =
        databaseWrapper.compileStatement(deleteStatementQuery)

    fun closeCompiledStatement() {
        compiledStatement?.close()
        compiledStatement = null
    }

    /**
     * @param databaseWrapper The database used to do an insert statement.
     * @return a new compiled [DatabaseStatement] representing insert.
     * To bind values use [.bindToInsertStatement].
     */
    fun getCompiledStatement(databaseWrapper: DatabaseWrapper): DatabaseStatement =
        databaseWrapper.compileStatement(compiledStatementQuery)

    /**
     * Creates a new [T] and Loads the cursor into a the object.
     *
     * @param cursor The cursor to load
     * @return A new [T]
     */
    fun loadFromCursor(cursor: FlowCursor, databaseWrapper: DatabaseWrapper): T =
        newInstance().apply { loadFromCursor(cursor, this, databaseWrapper) }

    override fun save(model: T, databaseWrapper: DatabaseWrapper): Boolean =
        modelSaver.save(model, databaseWrapper)

    override fun saveAll(models: Collection<T>, databaseWrapper: DatabaseWrapper): Long
        = listModelSaver.saveAll(models, databaseWrapper)

    override fun insert(model: T, databaseWrapper: DatabaseWrapper): Long =
        modelSaver.insert(model, databaseWrapper)

    override fun insertAll(models: Collection<T>, databaseWrapper: DatabaseWrapper): Long
        = listModelSaver.insertAll(models, databaseWrapper)

    override fun update(model: T, databaseWrapper: DatabaseWrapper): Boolean =
        modelSaver.update(model, databaseWrapper)

    override fun updateAll(models: Collection<T>, databaseWrapper: DatabaseWrapper): Long
        = listModelSaver.updateAll(models, databaseWrapper)

    override fun delete(model: T, databaseWrapper: DatabaseWrapper): Boolean =
        modelSaver.delete(model, databaseWrapper)

    override fun deleteAll(models: Collection<T>, databaseWrapper: DatabaseWrapper): Long
        = listModelSaver.deleteAll(models, databaseWrapper)

    override fun bindToInsertStatement(sqLiteStatement: DatabaseStatement, model: T) {
        bindToInsertStatement(sqLiteStatement, model, 0)
    }

    override fun bindToContentValues(contentValues: ContentValues, model: T) {
        bindToInsertValues(contentValues, model)
    }

    override fun bindToInsertValues(contentValues: ContentValues, model: T) {
        throw RuntimeException("ContentValues are no longer generated automatically. To enable it," +
            " set generateContentValues = true in @Table for $table.")
    }

    override fun bindToStatement(sqLiteStatement: DatabaseStatement, model: T) {
        bindToInsertStatement(sqLiteStatement, model, 0)
    }

    /**
     * If a [Model] has an auto-incrementing primary key, then
     * this method will be overridden.
     *
     * @param model The model object to store the key
     * @param id    The key to store
     */
    override fun updateAutoIncrement(model: T, id: Number) {

    }

    /**
     * @return The value for the [PrimaryKey.autoincrement]
     * if it has the field. This method is overridden when its specified for the [T]
     */
    override fun getAutoIncrementingId(model: T): Number? {
        throw RuntimeException("Table $table does not have an auto-incrementing id.")
    }

    fun hasAutoIncrement(model: T): Boolean {
        val id = getAutoIncrementingId(model) ?: throw IllegalStateException("An autoincrementing column field cannot be null.")

        return id.toLong() > 0
    }

    /**
     * Called when we want to save our [ForeignKey] objects. usually during insert + update.
     * This method is overridden when [ForeignKey] specified
     */
    open fun saveForeignKeys(model: T, wrapper: DatabaseWrapper) {

    }

    /**
     * Called when we want to delete our [ForeignKey] objects. During deletion [.delete]
     * This method is overridden when [ForeignKey] specified
     */
    open fun deleteForeignKeys(model: T, wrapper: DatabaseWrapper) {

    }

    override fun cachingEnabled(): Boolean = false

    var modelSaver: ModelSaver<T>
        get() {
            if (_modelSaver == null) {
                _modelSaver = createSingleModelSaver().apply { modelAdapter = this@ModelAdapter }
            }
            return _modelSaver!!
        }
        set(value) {
            this._modelSaver = value
            value.modelAdapter = this
        }

    protected open fun createSingleModelSaver(): ModelSaver<T> = ModelSaver()

    protected open fun createListModelSaver(): ListModelSaver<T> = ListModelSaver(modelSaver)

    /**
     * Retrieves a property by name from the table via the corresponding generated "_Table" class. Useful
     * when you want to dynamically get a property from an [ModelAdapter] and do an operation on it.
     *
     * @param columnName The column name of the property.
     * @return The property from the corresponding Table class.
     */
    abstract fun getProperty(columnName: String): Property<*>

    /**
     * @return When false, this table gets generated and associated with database, however it will not immediately
     * get created upon startup. This is useful for keeping around legacy tables for migrations.
     */
    open fun createWithDatabase(): Boolean = true

}