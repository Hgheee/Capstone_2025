export default function Input({ className = "", ...props }) {
  return (
    <input
      className={`w-full border rounded-md px-3 py-2 ${className}`}
      {...props}
    />
  );
}
