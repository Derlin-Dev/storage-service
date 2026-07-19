import { useAuth } from '../context/AuthContext.jsx'


export function Sidebar() {

  const { logout } = useAuth()


  return (

    <aside className="sidebar">


      <div className="sidebar-logo">
        MyStorage
      </div>


      <nav className="sidebar-menu">

        
        <button>
          📁 Mis archivos
        </button>
        {/* 

        <button>
          🔗 Compartidos
        </button>


        <button>
          ⭐ Favoritos
        </button>


        <button>
          🗑 Papelera
        </button>
        */}


      </nav>


    <button
        className="sidebar-logout"
        onClick={logout}
        >

        <span>
            🚪
        </span>

        Cerrar sesión

    </button>


    </aside>

  )
}