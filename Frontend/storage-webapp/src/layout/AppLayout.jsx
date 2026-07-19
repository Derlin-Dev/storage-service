import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar.jsx'
import { Navbar } from './Navbar.jsx'


export function AppLayout() {

  return (

    <div className="app-layout">

      <Sidebar />


      <div className="main-area">

        <Navbar />


        <main className="page-content">

          <Outlet />

        </main>


      </div>


    </div>

  )
}