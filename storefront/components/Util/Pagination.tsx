import { useRouter } from "next/router";
import NextArrow from "../../public/icons/NextArrow";
import PrevArrow from "../../public/icons/PrevArrow";
import { pushWithLang } from "../../lib/router-utils";

type Props = {
  lastPage: number;
  currentPage: number;
  orderby: "latest" | "price" | "price-desc";
};

const Pagination: React.FC<Props> = ({ lastPage, currentPage, orderby }) => {
  const router = useRouter();
  const { category } = router.query;
  const safeLastPage = Math.max(1, lastPage);
  const safeCurrentPage = Math.min(Math.max(1, currentPage), safeLastPage);

  let pageNumbers: number[] = [];

  for (let i = 1; i <= safeLastPage; i++) {
    pageNumbers.push(i);
  }

  let midPageNumbers = false;
  let startPageNumbers = false;
  let endPageNumbers = false;

  if (safeCurrentPage <= 2) {
    pageNumbers = [1, 2, 3];
    startPageNumbers = true;
    midPageNumbers = false;
    endPageNumbers = false;
  } else if (safeCurrentPage >= safeLastPage - 1) {
    pageNumbers = [safeLastPage - 2, safeLastPage - 1, safeLastPage];
    endPageNumbers = true;
    midPageNumbers = false;
    startPageNumbers = false;
  } else {
    pageNumbers = [safeCurrentPage - 1, safeCurrentPage, safeCurrentPage + 1];
    midPageNumbers = true;
    startPageNumbers = false;
    endPageNumbers = false;
  }

  if (safeLastPage === 3) {
    pageNumbers = [1, 2, 3];
    startPageNumbers = false;
    midPageNumbers = false;
    endPageNumbers = false;
  }

  if (safeLastPage === 1) {
    pageNumbers = [1];
    startPageNumbers = false;
    midPageNumbers = false;
    endPageNumbers = false;
  }

  pageNumbers = pageNumbers.filter((num) => num >= 1 && num <= safeLastPage);

  return (
    <div className="w-full">
      <ul className="flex justify-center">
        <li>
          <button
            type="button"
            aria-label="Navigate to Previous Page"
            onClick={() =>
              pushWithLang(
                router,
                `/product-category/${category}?page=${
                  safeCurrentPage - 1
                }&orderby=${orderby}`
              )
            }
            className={`${
              safeCurrentPage === 1
                ? "pointer-events-none cursor-not-allowed text-gray400"
                : "cursor-pointer"
            } focus:outline-none flex justify-center items-center h-10 w-16 px-3 border mx-1 hover:bg-gray500 hover:text-gray100`}
          >
            <PrevArrow />
          </button>
        </li>
        {(midPageNumbers || endPageNumbers) && (
          <li>
            <span className="flex items-end text-3xl">...</span>
          </li>
        )}
        {pageNumbers.map((num) => {
          return (
            <li key={num} className="">
              <button
                type="button"
                onClick={() =>
                  pushWithLang(
                    router,
                    `/product-category/${category}?page=${num}&orderby=${orderby}`
                  )
                }
                className={`${
                  num === safeCurrentPage && "bg-gray500 text-gray100"
                } focus:outline-none cursor-pointer flex justify-center items-center w-10 h-10 border mx-1 hover:bg-gray500 hover:text-gray100`}
              >
                {num}
              </button>
            </li>
          );
        })}
        {(midPageNumbers || startPageNumbers) && (
          <li>
            <span className="flex items-end text-3xl">...</span>
          </li>
        )}
        <li>
          <button
            type="button"
            aria-label="Navigate to Next Page"
            onClick={() =>
              pushWithLang(
                router,
                `/product-category/${category}?page=${
                  safeCurrentPage + 1
                }&orderby=${orderby}`
              )
            }
            className={`${
              safeCurrentPage >= safeLastPage
                ? "pointer-events-none cursor-not-allowed text-gray400"
                : "cursor-pointer"
            } focus:outline-none flex justify-center items-center h-10 w-16 px-3 border mx-1 hover:bg-gray500 hover:text-gray100`}
          >
            <NextArrow />
          </button>
        </li>
      </ul>
    </div>
  );
};

export default Pagination;
