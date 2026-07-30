package com.linkora.vm

import com.linkora.data.LinkItem

/**
 * Deshacer/rehacer por instantáneas: cada acción guarda el registro antes y después.
 * null significa "no existía", lo que cubre altas y bajas con el mismo mecanismo.
 */
class UndoStack(private val max: Int = 5) {

    data class Action(val label: String, val before: LinkItem?, val after: LinkItem?)

    private val undo = ArrayDeque<Action>()
    private val redo = ArrayDeque<Action>()

    val undoSize get() = undo.size
    val redoSize get() = redo.size

    fun push(label: String, before: LinkItem?, after: LinkItem?) {
        undo.addLast(Action(label, before, after))
        while (undo.size > max) undo.removeFirst()
        redo.clear()
    }

    /** Devuelve la acción a deshacer, ya movida a la pila de rehacer. */
    fun popUndo(): Action? = undo.removeLastOrNull()?.also {
        redo.addLast(it)
        while (redo.size > max) redo.removeFirst()
    }

    fun popRedo(): Action? = redo.removeLastOrNull()?.also {
        undo.addLast(it)
        while (undo.size > max) undo.removeFirst()
    }

    fun clear() { undo.clear(); redo.clear() }
}
