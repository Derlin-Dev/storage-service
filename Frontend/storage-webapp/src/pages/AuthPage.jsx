import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { login as loginRequest, signup as signupRequest } from '../api.js'

export function AuthPage() {
  const [authMode, setAuthMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', pass: '' })
  const [message, setMessage] = useState({ type: 'info', text: '' })
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirectTo = searchParams.get('redirect') || '/'

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage({ type: 'info', text: '' })

    try {
      if (authMode === 'signup') {
        await signupRequest({ name: form.name, email: form.email, pass: form.pass })
        setMessage({ type: 'success', text: 'Usuario registrado correctamente. Ahora puedes iniciar sesión.' })
        setAuthMode('login')
        setForm((current) => ({ ...current, name: '' }))
      } else {
        const payload = await loginRequest({ email: form.email, pass: form.pass })
        const token = payload?.data?.token

        if (!token) {
          throw new Error('El backend no devolvió un token válido.')
        }

        login(token)
        navigate(redirectTo, { replace: true })
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  return (

<main className="auth-page">


  <section className="auth-brand">


    <p className="eyebrow">
      STORAGE SERVICE
    </p>


    <h1>
      Tus archivos,
      siempre disponibles.
    </h1>


    <p className="hero-copy">

      Guarda, comparte y administra
      tus archivos desde cualquier lugar.

    </p>



    <div className="auth-features">


      <div>
        📂
        <span>
          Almacenamiento seguro
        </span>
      </div>


      <div>
        🔗
        <span>
          Comparte archivos fácilmente
        </span>
      </div>


      <div>
        🔒
        <span>
          Acceso protegido
        </span>
      </div>


    </div>


  </section>





  <section className="auth-card">


    <div className="panel-heading">


      <div>

        <p className="eyebrow">
          Cuenta
        </p>


        <h2>
          {
            authMode === 'login'
            ? 'Iniciar sesión'
            : 'Crear cuenta'
          }
        </h2>

      </div>



      <div className="toggle-group">


        <button

          type="button"

          className={
            authMode === 'login'
            ? 'toggle active'
            : 'toggle'
          }

          onClick={() => setAuthMode('login')}

        >
          Login

        </button>



        <button

          type="button"

          className={
            authMode === 'signup'
            ? 'toggle active'
            : 'toggle'
          }

          onClick={() => setAuthMode('signup')}

        >

          Registro

        </button>



      </div>


    </div>




    {
      redirectTo !== '/' && (

        <p className="section-copy">

          Inicia sesión para continuar.

        </p>

      )
    }





    <form

      onSubmit={handleSubmit}

      className="stacked-form"

    >


      {
        authMode === 'signup' && (

          <label>

            Nombre

            <input

              type="text"

              value={form.name}

              onChange={
                e =>
                setForm({
                  ...form,
                  name:e.target.value
                })
              }

              placeholder="Tu nombre"

              required

            />

          </label>

        )
      }




      <label>

        Correo electrónico


        <input

          type="email"

          value={form.email}

          onChange={
            e =>
            setForm({
              ...form,
              email:e.target.value
            })
          }


          placeholder="correo@ejemplo.com"

          required

        />


      </label>





      <label>

        Contraseña


        <input

          type="password"

          value={form.pass}

          onChange={
            e =>
            setForm({
              ...form,
              pass:e.target.value
            })
          }


          placeholder="••••••••"

          required

        />


      </label>





      <button

        className="primary-btn"

        disabled={loading}

      >

        {
          loading
          ? "Procesando..."
          :
          authMode === 'login'
          ? "Ingresar"
          : "Crear cuenta"
        }


      </button>



    </form>




    {
      message.text && (

        <div className={`message ${message.type}`}>

          {message.text}

        </div>

      )
    }



  </section>



</main>

)
}
