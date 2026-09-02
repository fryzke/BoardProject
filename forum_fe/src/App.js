import './App.css';
import { BrowserRouter, Routes, Route } from "react-router-dom";
import SignUpPage from "./Pages/SignUp/SignUpPage";
import ForumPage from "./Pages/Forum/ForumPage";
import SignInPage from './Pages/SignIn/SignInPage';

import PostDetailPage from "./Pages/Post/PostDetailPage";
import PostEditPage from "./Pages/Post/PostEditPage";
import ProtectedRoute from "./ProtectedRoutes";

function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<ForumPage />} />
          <Route path="/sign-up" element={<SignUpPage />} />
          <Route path="/sign-in" element={<SignInPage />} />
          <Route path="/post/:id" element={<PostDetailPage />} />
          <Route path="/write" element={
            <ProtectedRoute>
              <PostEditPage />
            </ProtectedRoute>
          } />
          <Route path="/edit/:id" element={
            <ProtectedRoute>
              <PostEditPage />
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
