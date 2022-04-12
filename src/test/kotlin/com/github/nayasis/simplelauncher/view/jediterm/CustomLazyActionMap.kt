package com.github.nayasis.simplelauncher.view.jediterm

import java.lang.reflect.InvocationTargetException
import javax.swing.Action
import javax.swing.ActionMap
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.ActionMapUIResource

class CustomLazyActionMap: ActionMapUIResource {

    companion object {
        /**
         * Installs an ActionMap that will be populated by invoking the
         * `loadActionMap` method on the specified Class
         * when necessary.
         *
         *
         * This should be used if the ActionMap can be shared.
         *
         * @param c JComponent to install the ActionMap on.
         * @param loaderClass Class object that gets loadActionMap invoked
         * on.
         * @param defaultsKey Key to use to defaults table to check for
         * existing map and what resulting Map will be registered on.
         */
        fun installLazyActionMap(
            c: JComponent?, loaderClass: Class<*>,
            defaultsKey: String?
        ) {
            var map = UIManager.get(defaultsKey) as ActionMap
            if (map == null) {
                map = CustomLazyActionMap(loaderClass)
                UIManager.getLookAndFeelDefaults()[defaultsKey] = map
            }
            SwingUtilities.replaceUIActionMap(c, map)
        }
    }


    constructor()
    constructor(loader: Class<*>) {
        _loader = loader
    }

    /**
     * Object to invoke `loadActionMap` on. This may be
     * a Class object.
     */
    @Transient
    private var _loader: Any? = null

    /**
     * Returns an ActionMap that will be populated by invoking the
     * `loadActionMap` method on the specified Class
     * when necessary.
     *
     *
     * This should be used if the ActionMap can be shared.
     *
     * @param loaderClass Class object that gets loadActionMap invoked
     * on.
     * @param defaultsKey Key to use to defaults table to check for
     * existing map and what resulting Map will be registered on.
     */
    fun getActionMap(
        loaderClass: Class<*>,
        defaultsKey: String?
    ): ActionMap? {
        var map = UIManager.get(defaultsKey) as ActionMap
        if (map == null) {
            map = CustomLazyActionMap(loaderClass)
            UIManager.getLookAndFeelDefaults()[defaultsKey] = map
        }
        return map
    }


    fun put(action: Action) {
        put(action.getValue(Action.NAME), action)
    }

    override fun put(key: Any?, action: Action?) {
        loadIfNecessary()
        super.put(key, action)
    }

    override fun get(key: Any?): Action? {
        loadIfNecessary()
        return super.get(key)
    }

    override fun remove(key: Any?) {
        loadIfNecessary()
        super.remove(key)
    }

    override fun clear() {
        loadIfNecessary()
        super.clear()
    }

    override fun keys(): Array<Any?>? {
        loadIfNecessary()
        return super.keys()
    }

    override fun size(): Int {
        loadIfNecessary()
        return super.size()
    }

    override fun allKeys(): Array<Any?>? {
        loadIfNecessary()
        return super.allKeys()
    }

    override fun setParent(map: ActionMap?) {
        loadIfNecessary()
        super.setParent(map)
    }

    private fun loadIfNecessary() {
        if (_loader != null) {
            val loader = _loader
            _loader = null
            val klass = loader as Class<*>
            try {
                val method = klass.getDeclaredMethod(
                    "loadActionMap",
                    *arrayOf<Class<*>>(CustomLazyActionMap::class.java)
                )
                method.invoke(klass, *arrayOf<Any>(this))
            } catch (nsme: NoSuchMethodException) {
                assert(false) {
                    "LazyActionMap unable to load actions " +
                        klass
                }
            } catch (iae: IllegalAccessException) {
                assert(false) {
                    "LazyActionMap unable to load actions " +
                        iae
                }
            } catch (ite: InvocationTargetException) {
                assert(false) {
                    "LazyActionMap unable to load actions " +
                        ite
                }
            } catch (iae: IllegalArgumentException) {
                assert(false) {
                    "LazyActionMap unable to load actions " +
                        iae
                }
            }
        }
    }

}