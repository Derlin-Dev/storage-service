import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './App.css'

import { AuthProvider } from './context/AuthContext.jsx'
import { ProtectedRoute } from './components/ProtectedRoute.jsx'

import { AuthPage } from './pages/AuthPage.jsx'
import { DashboardPage } from './pages/DashboardPage.jsx'
import { SharedFilePage } from './pages/SharedFilePage.jsx'

import { AppLayout } from './layout/AppLayout.jsx'


function App() {


  return (

    <AuthProvider>


      <BrowserRouter>


        <Routes>


          <Route
            path="/login"
            element={<AuthPage />}
          />



          <Route

            element={

              <ProtectedRoute>

                <AppLayout />

              </ProtectedRoute>

            }

          >


            <Route
              path="/"
              element={<DashboardPage />}
            />



            <Route
              path="/files/:fileId"
              element={<SharedFilePage />}
            />


          </Route>



        </Routes>


      </BrowserRouter>


    </AuthProvider>

  )

}


export default App
