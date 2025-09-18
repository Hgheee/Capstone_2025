export default function Button({
  as: Tag = "button",
  className = "",
  ...props
}) {
  return (
    <Tag
      className={`px-4 py-2 rounded-md border hover:bg-gray-100 active:scale-[.99] ${className}`}
      {...props}
    />
  );
}
